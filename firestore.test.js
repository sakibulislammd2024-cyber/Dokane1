const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_123";
const STAFF_UID = "staff_456";
const OUTSIDER_UID = "outsider_789";
const STORE_ID = "store_abc";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedStore() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("stores").doc(STORE_ID).set({
      name: "Bismillah Store",
      phone: "01712345678",
      address: "Dhaka",
      shopType: "grocery",
      productCategories: ["Grocery"],
      ownerId: OWNER_UID,
      memberUids: [OWNER_UID],
      members: [{ userId: OWNER_UID, role: "owner", status: "active" }],
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedStore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(OWNER_UID).get());
  await assertFails(unauthDb.collection("stores").doc(STORE_ID).get());
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );
});

test("User profile: owner can read/write own profile, outsider cannot", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).set({
      email: "owner@store.com",
      storeId: STORE_ID,
      role: "owner",
      isSimpleMode: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(ownerDb.collection("users").doc(OWNER_UID).get());
  await assertFails(outsiderDb.collection("users").doc(OWNER_UID).get());
  await assertFails(
    outsiderDb.collection("users").doc(OWNER_UID).set({
      email: "hacked@store.com",
      storeId: "other",
    })
  );
});

test("Store & operational subcollections: member allowed, outsider denied", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await assertSucceeds(ownerDb.collection("stores").doc(STORE_ID).get());
  await assertFails(outsiderDb.collection("stores").doc(STORE_ID).get());

  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_1").set({
      storeId: STORE_ID,
      name: "Miniket Rice 5kg",
      sellPrice: 375,
      stock: 20,
    })
  );
  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(STORE_ID).collection("products").doc("prod_2").set({
      storeId: STORE_ID,
      name: "Unauthorized",
    })
  );
});

test("Private AI key is strictly owner-only", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("stores").doc(STORE_ID).set({
      name: "Bismillah Store",
      ownerId: OWNER_UID,
      memberUids: [OWNER_UID, STAFF_UID],
    });
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").set({
      storeId: STORE_ID,
      apiKey: "AIzaTestKey1234567890",
      keyLast4: "7890",
      updatedBy: OWNER_UID,
    })
  );
  await assertSucceeds(
    ownerDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").get()
  );
  await assertFails(
    staffDb.collection("stores").doc(STORE_ID).collection("private").doc("aiKey").set({
      storeId: STORE_ID,
      apiKey: "AIzaStaffKey1234567890",
      keyLast4: "7890",
      updatedBy: STAFF_UID,
    })
  );
});

test("Invite code creation (active), owner-scoped list, and atomic staff join work securely", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("invites").doc("AB12CD").set({
      code: "AB12CD",
      storeId: STORE_ID,
      storeName: "Bismillah Store",
      staffName: "Karim",
      roleLabel: "Cashier",
      permissionLevel: "Limited",
      status: "active",
      createdByUid: OWNER_UID,
      createdAt: Date.now(),
      expiresAt: Date.now() + 3600000,
    })
  );

  // Owner-scoped query matches StoreRepository.observeInvites
  await assertSucceeds(
    ownerDb
      .collection("invites")
      .where("storeId", "==", STORE_ID)
      .where("createdByUid", "==", OWNER_UID)
      .get()
  );

  // Unfiltered or outsider list query must fail
  await assertFails(outsiderDb.collection("invites").where("storeId", "==", STORE_ID).get());
  await assertFails(ownerDb.collection("invites").get());

  await assertSucceeds(staffDb.collection("invites").doc("AB12CD").get());
  await assertSucceeds(
    staffDb.collection("invites").doc("AB12CD").update({
      status: "used",
      usedByUid: STAFF_UID,
      usedByEmail: "staff@store.com",
      usedAt: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
