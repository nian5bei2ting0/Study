const fs = require("fs");
const path = process.argv[2];
const needle = process.argv[3];
const c = fs.readFileSync(path, "utf8");
const j = c.indexOf(needle);
console.log("idx", j);
if (j < 0) process.exit(1);
console.log(c.substring(Math.max(0, j - 800), j + 7000));
