// https://docs.mongodb.com/manual/reference/method/db.createCollection/
// https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/

db.createCollection("users");
db.users.createIndex({ username: 1 }, { name: "uq_username", unique: true });
