// https://docs.mongodb.com/manual/reference/method/db.createCollection/
// https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/
// https://docs.mongodb.com/manual/reference/method/db.collection.updateOne/#mongodb-method-db.collection.updateOne

db.createCollection("users");
db.users.createIndex({ username: 1 }, { name: "uq_username", unique: true });

// DO NOT REMOVE! This line will keep track migration file.
db.migration.updateOne({ collection: "security" }, { "$set": { version: "1.0.0" } }, { upsert: true })
