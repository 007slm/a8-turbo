#!/bin/bash
echo "Checking Archive Log Mode..."
status=$(sqlplus -s / as sysdba <<EOF
set heading off
set feedback off
select log_mode from v\$database;
exit;
EOF
)

if [[ $status == *"NOARCHIVELOG"* ]]; then
  echo "Enabling Archive Log Mode..."
  sqlplus / as sysdba <<EOF
    SHUTDOWN IMMEDIATE;
    STARTUP MOUNT;
    ALTER DATABASE ARCHIVELOG;
    ALTER DATABASE OPEN;
    ALTER PLUGGABLE DATABASE ALL OPEN;
    exit;
EOF
  echo "Archive Log Mode enabled."
else
  echo "Archive Log Mode is already enabled."
fi
