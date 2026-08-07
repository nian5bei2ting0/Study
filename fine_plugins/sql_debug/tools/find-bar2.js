const fs = require("fs");
const dir = "E:/AI/cursor/fine_plugins/sql_debug/com/fr/web/resources/dist/";
for (const f of fs.readdirSync(dir)) {
  if (!f.endsWith(".min.js")) continue;
  const c = fs.readFileSync(dir + f, "utf8");
  if (!c.includes("multi_select_no_bar")) continue;
  const j = c.indexOf("multi_select_no_bar_combo");
  if (j < 0) continue;
  const slice = c.substring(j, j + 5000);
  if (slice.includes("setValue:function") && slice.includes("getText")) {
    console.log("FILE:", f);
    console.log(slice.substring(0, 4000));
    break;
  }
}
