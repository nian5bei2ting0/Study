const fs = require("fs");
const dir = "E:/AI/cursor/fine_plugins/sql_debug/com/fr/web/resources/dist/";
for (const f of fs.readdirSync(dir)) {
  if (!f.endsWith(".min.js")) continue;
  const c = fs.readFileSync(dir + f, "utf8");
  const key = "multi_select_no_bar";
  if (!c.includes(key)) continue;
  let i = 0;
  while ((j = c.indexOf(key, i)) >= 0) {
    const s = c.substring(j, j + 3500);
    if (s.includes("shortcut") && s.includes("setValue")) {
      console.log("FILE", f, "at", j);
      console.log(s.slice(0, 3200));
      process.exit(0);
    }
    i = j + 20;
  }
}
console.log("not found");
