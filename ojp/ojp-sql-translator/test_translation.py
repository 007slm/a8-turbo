import sqlglot

sql = "SELECT * FROM users WHERE created_at < SYSDATE"
source = "oracle"
target = "starrocks"

translated = sqlglot.transpile(sql, read=source, write=target)[0]
print(f"Original: {sql}")
print(f"Translated: {translated}")

if "NOW()" in translated or "CURRENT_TIMESTAMP" in translated:
    print("Verification Successful: SYSDATE translated to equivalent.")
else:
    print("Verification Result: Check manually.")
