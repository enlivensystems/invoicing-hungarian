#!/bin/bash

# When you are developing on your local computer, change `KEYSTOREFILE` temporarily
# and run the script in order to have "nav.gov.hu" in your trust-store.
#
# Do not modify, push the script to master branch otherwise. It will mess up Bamboo build
# as well as Dockerfile build. All of those depend on this very script.

HOST=api-test.onlineszamla.nav.gov.hu
PORT=443
KEYSTOREFILE="${JAVA_HOME}/lib/security/cacerts"
# You rarely have a different password.
KEYSTOREPASS=changeit

# Get the SSL certificate.
openssl s_client -connect ${HOST}:${PORT} </dev/null \
    | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${HOST}.cert

# Create a keystore and import certificate.
keytool -import -noprompt -trustcacerts \
    -alias ${HOST} -file ${HOST}.cert \
    -keystore "${KEYSTOREFILE}" -storepass ${KEYSTOREPASS}

# Verify we've got it.
keytool -list -v -keystore "${KEYSTOREFILE}" -storepass ${KEYSTOREPASS} -alias ${HOST}