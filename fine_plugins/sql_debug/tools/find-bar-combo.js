const fs = require("fs");
const dir = "E:/AI/cursor/fine_plugins/sql_debug/com/fr/web/resources/dist/";
const files = fs.readdirSync(dir).filter((f) => f.endsWith(".min.js"));
const k = 'BI.shortcut("bi.multi_select_no_bar_combo"';
for (const f of files) {
  const c = fs.readFileSync(dir + f, "utf8");
  const j = c.indexOf(k);
  if (j >= 0) {
    console.log("FILE", f, "IDX", j);
    console.log(c.substring(j, j + 3500));
    break;
  }
}
