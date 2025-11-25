#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle and Java processes.
# For declarative memory configuration, you can use JAVA_TOOL_OPTIONS which will be picked up by both Java and Gradle processes.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if ${cygwin} ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done

APP_HOME=`dirname "$PRG"`

# Absolutize APP_HOME
# This allows the gradlew script to be invoked from its parent directory
# while symbolic linking destination is not resolved correctly
if [ "$APP_HOME" = "." ]; then
  APP_HOME=`pwd`
fi
APP_HOME=`cd "$APP_HOME" && pwd`

# Check for wrapper properties.
WRAPPER_PROPS="${APP_HOME}/gradle/wrapper/gradle-wrapper.properties"
if [ ! -f "${WRAPPER_PROPS}" ]; then
    echo "ERROR: Could not find wrapper properties file." >&2
    echo "${WRAPPER_PROPS}" >&2
    exit 1
fi

# Read wrapper properties.
GRADLE_USER_HOME=""
while read -r prop; do
    case "${prop}" in
        distributionUrl=*)
            DISTRIBUTION_URL_PROPERTY="${prop}"
            ;;
        zipStoreBase=*)
            ZIP_STORE_BASE_PROPERTY="${prop}"
            ;;
        zipStorePath=*)
            ZIP_STORE_PATH_PROPERTY="${prop}"
            ;;
        distributionSha256Sum=*)
            DISTRIBUTION_SHA_256_SUM_PROPERTY="${prop}"
            ;;
        gradle_user_home=*)
            GRADLE_USER_HOME_PROPERTY="${prop}"
            ;;
        *)
            # ignore
            ;;
    esac
done < "${WRAPPER_PROPS}"

# Set GRADLE_USER_HOME.
# Try from properties, then from env variables, then use default.
if [ -n "${GRADLE_USER_HOME_PROPERTY}" ]; then
    GRADLE_USER_HOME=`echo "${GRADLE_USER_HOME_PROPERTY}" | cut -d'=' -f2`
    # For Cygwin, ensure paths are in UNIX format before anything is touched.
    if ${cygwin} ; then
        GRADLE_USER_HOME=`cygpath --unix "${GRADLE_USER_HOME}"`
    fi
elif [ -n "$GRADLE_USER_HOME" ] ; then
    # Use GRADLE_USER_HOME from env variables.
    echo "Using GRADLE_USER_HOME from environment variable."
elif [ -n "$HOME" ] ; then
    GRADLE_USER_HOME="${HOME}/.gradle"
else
    GRADLE_USER_HOME="/tmp/.gradle"
fi
export GRADLE_USER_HOME

# Make sure the Gradle user home directory exist.
if [ ! -d "${GRADLE_USER_HOME}" ]; then
    mkdir -p "${GRADLE_USER_HOME}"
    if [ $? -ne 0 ]; then
        echo "ERROR: Could not create directory ${GRADLE_USER_HOME}" >&2
        exit 1
    fi
fi

# Determine the name of the distribution directory.
# This is determined by the last path element of the distribution URL.
# We also remove the '-bin' or '-all' suffix from the name.
DISTRIBUTION_URL=`echo "${DISTRIBUTION_URL_PROPERTY}" | cut -d'=' -f2`
DISTRIBUTION_PATH=`echo "${DISTRIBUTION_URL}" | sed -e 's|.*/\(.*\)|\1|'`
DISTRIBUTION_NAME=`echo "${DISTRIBUTION_PATH}" | sed -e 's/\(.*\)-[a-z]*.zip/\1/'`
DISTRIBUTION_DIR="${GRADLE_USER_HOME}/wrapper/dists/${DISTRIBUTION_NAME}"
DISTRIBUTION_SHA_256_SUM=`echo "${DISTRIBUTION_SHA_256_SUM_PROPERTY}" | cut -d'=' -f2`

# Determine the location of the distribution zip file.
# This is determined by the zipStoreBase and zipStorePath properties.
# If these are not specified, we use the default of GRADLE_USER_HOME.
if [ -n "${ZIP_STORE_BASE_PROPERTY}" ]; then
    ZIP_STORE_BASE=`echo "${ZIP_STORE_BASE_PROPERTY}" | cut -d'=' -f2`
    if [ "${ZIP_STORE_BASE}" = "GRADLE_USER_HOME" ]; then
        ZIP_STORE_BASE="${GRADLE_USER_HOME}"
    fi
else
    ZIP_STORE_BASE="${GRADLE_USER_HOME}"
fi
if [ -n "${ZIP_STORE_PATH_PROPERTY}" ]; then
    ZIP_STORE_PATH=`echo "${ZIP_STORE_PATH_PROPERTY}" | cut -d'=' -f2`
else
    ZIP_STORE_PATH="wrapper/dists"
fi
ZIP_STORE_DIR="${ZIP_STORE_BASE}/${ZIP_STORE_PATH}/${DISTRIBUTION_NAME}"

# Create the distribution directory if it does not exist.
if [ ! -d "${DISTRIBUTION_DIR}" ]; then
    mkdir -p "${DISTRIBUTION_DIR}"
    if [ $? -ne 0 ]; then
        echo "ERROR: Could not create directory ${DISTRIBUTION_DIR}" >&2
        exit 1
    fi
fi

# Create the zip store directory if it does not exist.
if [ ! -d "${ZIP_STORE_DIR}" ]; then
    mkdir -p "${ZIP_STORE_DIR}"
    if [ $? -ne 0 ]; then
        echo "ERROR: Could not create directory ${ZIP_STORE_DIR}" >&2
        exit 1
    fi
fi

# Determine the location of the distribution zip file.
DISTRIBUTION_ZIP="${ZIP_STORE_DIR}/${DISTRIBUTION_PATH}"

# Download the distribution if it does not exist.
if [ ! -f "${DISTRIBUTION_ZIP}" ]; then
    echo "Downloading ${DISTRIBUTION_URL}"
    # Download to a temporary file and then move it to the final destination.
    # This avoids truncated downloads when the script is interrupted.
    TMP_DISTRIBUTION_ZIP="${DISTRIBUTION_ZIP}.tmp"
    if command -v "wget" > /dev/null; then
        wget --quiet --output-document "${TMP_DISTRIBUTION_ZIP}" "${DISTRIBUTION_URL}"
    elif command -v "curl" > /dev/null; then
        # The --fail option of curl will make it exit with an error code if the download fails.
        # The --location option will make it follow redirects.
        # The --silent option will prevent it from printing progress information.
        # The --show-error option will make it print an error message if the download fails.
        # The --output option will specify the output file.
        curl --fail --location --silent --show-error --output "${TMP_DISTRIBUTION_ZIP}" "${DISTRIBUTION_URL}"
    else
        echo "ERROR: Neither wget nor curl is available." >&2
        exit 1
    fi
    if [ $? -ne 0 ]; then
        echo "ERROR: Download failed." >&2
        exit 1
    fi
    mv "${TMP_DISTRIBUTION_ZIP}" "${DISTRIBUTION_ZIP}"
    if [ $? -ne 0 ]; then
        echo "ERROR: Could not move temporary file to final destination." >&2
        exit 1
    fi
fi

# Verify the integrity of the downloaded distribution.
# The SHA-256 sum of the downloaded file is compared with the expected value.
if [ -n "${DISTRIBUTION_SHA_256_SUM}" ]; then
    if command -v "sha256sum" > /dev/null; then
        DOWNLOADED_SHA_256_SUM=`sha256sum "${DISTRIBUTION_ZIP}" | cut -d' ' -f1`
    elif command -v "shasum" > /dev/null; then
        DOWNLOADED_SHA_256_SUM=`shasum -a 256 "${DISTRIBUTION_ZIP}" | cut -d' ' -f1`
    else
        echo "ERROR: Neither sha256sum nor shasum is available." >&2
        exit 1
    fi
    if [ "${DOWNLOADED_SHA_256_SUM}" != "${DISTRIBUTION_SHA_256_SUM}" ]; then
        echo "ERROR: The SHA-256 sum of the downloaded file does not match the expected value." >&2
        echo "Expected: ${DISTRIBUTION_SHA_256_SUM}" >&2
        echo "Actual:   ${DOWNLOADED_SHA_256_SUM}" >&2
        exit 1
    fi
fi

# Unpack the distribution.
# The distribution is unpacked to a temporary directory and then moved to the final destination.
# This avoids a partially unpacked distribution when the script is interrupted.
TMP_DISTRIBUTION_UNPACK_DIR="${DISTRIBUTION_DIR}.tmp"
rm -rf "${TMP_DISTRIBUTION_UNPACK_DIR}"
mkdir "${TMP_DISTRIBUTION_UNPACK_DIR}"
if [ $? -ne 0 ]; then
    echo "ERROR: Could not create temporary directory ${TMP_DISTRIBUTION_UNPACK_DIR}" >&2
    exit 1
fi
unzip -q "${DISTRIBUTION_ZIP}" -d "${TMP_DISTRIBUTION_UNPACK_DIR}"
if [ $? -ne 0 ]; then
    echo "ERROR: Could not unpack distribution." >&2
    exit 1
fi
# Get the name of the unpacked directory.
# The distribution should contain a single directory.
UNPACKED_DIR_NAME=`ls -1 "${TMP_DISTRIBUTION_UNPACK_DIR}"`
UNPACKED_DIR="${TMP_DISTRIBUTION_UNPACK_DIR}/${UNPACKED_DIR_NAME}"
# Move the unpacked directory to the final destination.
mv "${UNPACKED_DIR}" "${DISTRIBUTION_DIR}"
if [ $? -ne 0 ]; then
    echo "ERROR: Could not move temporary directory to final destination." >&2
    exit 1
fi
rm -rf "${TMP_DISTRIBUTION_UNPACK_DIR}"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum number of open file descriptors.
if [ "$darwin" = "false" ] && [ "$cygwin" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ] ; then
            # Use the system limit
            MAX_FD="$MAX_FD_LIMIT"
        fi

        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            echo "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        echo "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# Collect all arguments for the java command, following the shell quoting and substitution rules
#
# It has been noticed that different shells will split the arguments differently.
# This is why we need to collect all arguments into a single array, and then pass them to java.
#
# See https://github.com/gradle/gradle/issues/22557 and https://github.com/gradle/gradle/issues/13845
# for more details.
#
declare -a java_args
declare -a gradle_args

# Split arguments into jvm and gradle args.
# It is recommended to use the `org.gradle.jvmargs` property in `gradle.properties` file to set JVM options.
# For more details, see https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties
#
# If the `org.gradle.jvmargs` property is not set, the `DEFAULT_JVM_OPTS` variable is used.
#
# Any arguments starting with `-D` or `-X` are considered JVM options.
# All other arguments are considered Gradle options.
#
for arg in "$@"; do
    case $arg in
    -D*|-X*)
      java_args=("${java_args[@]}" "$arg")
      ;;
    *)
      gradle_args=("${gradle_args[@]}" "$arg")
      ;;
    esac
done

# Add default JVM options.
# The `org.gradle.jvmargs` property in `gradle.properties` file is used to set JVM options.
# If this property is not set, the `DEFAULT_JVM_OPTS` variable is used.
#
# For more details, see https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties
if [ -z "${org_gradle_jvmargs}" ]; then
    org_gradle_jvmargs="${DEFAULT_JVM_OPTS}"
fi

# The `org.gradle.jvmargs` property can contain multiple JVM options, separated by spaces.
# We need to split the string into an array of options.
# We use the `read` command to do this, as it is the most reliable way to split a string by spaces.
#
# The `read` command will read from the standard input.
# We use the `<<<` operator to redirect the string to the standard input of the `read` command.
#
# The `-r` option of the `read` command will prevent backslash escapes from being interpreted.
# The `-a` option of the `read` command will store the words in an array.
#
read -r -a jvmargs <<< "${org_gradle_jvmargs}"
java_args=("${java_args[@]}" "${jvmargs[@]}")

# Add the application jar to the classpath.
CLASSPATH="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"

# Execute the application.
exec "$JAVACMD" "${java_args[@]}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "${gradle_args[@]}"
