// https://docs.mongodb.com/manual/reference/method/db.createCollection/
// https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/
// https://docs.mongodb.com/manual/reference/method/db.collection.updateOne/#mongodb-method-db.collection.updateOne

db.permissions.drop();
db.permissions.insert({ "_id": "security:user" });
db.permissions.insert({ "_id": "security:graphql" });

// DO NOT REMOVE! This line will keep track migration file.
db.migration.updateOne({ database: "security" }, { "$set": { version: "1.2.0" } }, { upsert: true });
