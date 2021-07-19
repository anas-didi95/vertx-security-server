// https://docs.mongodb.com/manual/reference/method/db.createCollection/
// https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/
// https://docs.mongodb.com/manual/reference/method/db.collection.updateOne/#mongodb-method-db.collection.updateOne

db.tokens.drop();
db.tokens.createIndex({ "issuedDate": 1 }, { name: "issuedDate#1", expireAfterSeconds: 2592000 }) // 30 days

// DO NOT REMOVE! This line will keep track migration file.
db.migration.updateOne({ database: "security" }, { "$set": { version: "1.1.0" } }, { upsert: true })
