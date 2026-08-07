const fs = require("fs");
const c = fs.readFileSync(process.argv[2], "utf8");
const needle = "multi_select_no_bar_combo";
let i = 0;
let n = 0;
while ((j = c.indexOf(needle, i)) >= 0 && n < 8) {
  const s = c.substring(j, j + 4000);
  if (s.includes("setValue:function") && s.includes("getValue:function")) {
    console.log("---", j, "---");
    console.log(s.slice(0, 3500));
    n++;
  }
  i = j + 30;
}
