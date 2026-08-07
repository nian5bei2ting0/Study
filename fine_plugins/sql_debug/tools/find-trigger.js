const fs = require("fs");
const c = fs.readFileSync("E:/AI/cursor/fine_plugins/sql_debug/com/fr/web/resources/dist/base.min.js", "utf8");
const needles = ["multi_select_no_bar", "MultiSelectCombo", "_digest", "请选择"];
for (const needle of needles) {
  let i = 0;
  let n = 0;
  while ((j = c.indexOf(needle, i)) >= 0 && n < 2) {
    const s = c.substring(j, j + 2000);
    if (s.includes("setValue") || s.includes("setText") || s.includes("getText")) {
      console.log("\n===", needle, "@", j, "===");
      console.log(s.slice(0, 1800));
      n++;
    }
    i = j + needle.length;
  }
}
