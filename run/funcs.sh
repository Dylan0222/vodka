# ----
# $1 is the properties file
# ----
PROPS=$1
if [ ! -f ${PROPS} ]; then
  echo "${PROPS}: no such file" >&2
  exit 1
fi

DIR=../target
if [ "$(ls -A ${DIR})" == "" ]; then
  echo "target dir is empty"
else
  rm ../dist/*.jar
  cp ${DIR}/*dependencies.jar ../dist/
fi
# ----
# getProp()
#
#   Get a config value from the properties file.
# ----
function getProp() {
  grep "^${1}=" ${PROPS} | sed -e "s/^${1}=//"
}

# ----
# getCP()
#
#   Determine the CLASSPATH based on the database system.
# ----
function setCP() {
  cp=""
  myCP=".:${cp}:../dist/*"
  export myCP
}

# ----
# Make sure that the properties file does have db= and the value
# is a database, we support.
# ----
case "$(getProp db)" in
firebird | oracle | postgres | oceanbase | tidb | polardb) ;;

"")
  echo "ERROR: missing db= config option in ${PROPS}" >&2
  exit 1
  ;;
*)
  echo "ERROR: unsupported database type 'db=$(getProp db)' in ${PROPS}" >&2
  exit 1
  ;;
esac
