*Signing modules via PKCS#11/HSM keystores is an incubating feature. See the note about the `certFile` setting below. The initial implementation was validated against a YubiKey 5 NFC.*

# Signing Via Hardware Security Module
It's possible to sign a module using a PKCS#11 keystore, which is usually a hardware token such as a YubiKey. (There are software PKCS#11 keystores as well, though they are more common for testing in that they do not have the physical attributes that make for secure HSMs.)

This is an alternative to PKCS#12 file-base keystores that typically reside in the subdirectory of a user's home directory.

## Prerequesites
You should have the following:

* A hardware (or software) PKCS#11 compliant keystore that supports the [Java `SunPKCS11Provider`](https://docs.oracle.com/en/java/javase/17/security/pkcs11-reference-guide1.html#GUID-6DA72F34-6C6A-4F7D-ADBA-5811576A9331).
* A PKCS#11 driver (or module) locally on your build workstation or on a build server, depending on the use case. Your HSM may be compatible with the [OpenSC Minidriver](https://github.com/OpenSC/OpenSC/wiki), which supports a wide variety of hardware tokens. Note that you may not get full support for `SunPKCS11Provider` unless you use a driver from the HSM vendor. For example, YubiKey provides the [YKCS11 driver](https://developers.yubico.com/yubico-piv-tool/YKCS11/) as part of its `yubico-piv-tool` a.k.a. YubiKey Manager application.
* The `SunPKCS11Provider` requires you have a PKCS#11 configuration file specifying, among other things, the location of that driver on your filesystem. You can see examples of such files for [YubiKeys on Windows](gradle-module-plugin/src/functionalTest/resources/certs/pkcs11-yk5-win.cfg) and for [OpenSC on Linux](gradle-module-plugin/src/functionalTest/resources/certs/pkcs11.cfg) in this repository.
* Unless your HSM already contains your signing key(s), you may need a management application such as the YubiKey Manager to generate a keypair on your device. You may also be able to use lower-level tools like `pkcs11-tool` that is bundled with the OpenSC tool suite or `keytool` that comes with Java to--JRE or JDK--to generate a keypair.

## Steps
The first few steps here are heavily dependent on your HSM's support of generic key management tools like OpenSC `pkcs11-tool` or Java `keytool`, or alternatively the vendor's key management tool.

It is often the case however that even if you cannot generate (or import) a keypair with the generic tools, you may be able to list key information from the HSM with those tools.

1. Generate or import a SHA256 with RSA-type keypair using either the generic or vendor-specific key management tool. (Currently, the `module-signer` library called by the plugin looks [only for private keys with the `SHA256withRSA` algorithm type](https://github.com/inductiveautomation/module-signer/blob/master/src/main/java/com/inductiveautomation/ignitionsdk/ModuleSigner.java/#L95). Support for different key algorithms is in the planning stage.)
2. If you do not already have a cert file for the key on the filesystem, use the key management to export it from the keystore onto the filesystem. For purposes of module signing, the cert can be self-signed. It need not come from a Certificate Authority. In the future the plugin may be able to retrieve the cert from the HSM directly.
3. Retrieve the key alias using one of the key management tools, ideally via `keytool -list`. Note that your HSM may contain multiple private keys even if you only generated or imported a single signing key in step 1. With some HSMs cases you may be able to specify the key alias yourself during generation or import, and in other cases the HSM or the vendor key manager may hardcode it. Either way you need to note that alias for the next step.
4. Whether in a `gradle.properties` file or via command flags, sign your module as follows. We'll use command flags for clarity. Note how we do *not* pass `keystoreFile` as the private signing key is in the HSM. However PKCS#11 keystores almost universally require a PIN to "unlock" them, so `keystorePassword` is still required. 

```bash
$ gradlew :signModule \
    --certAlias modsigning \
    --certFile ~/.ssl/modsigning.cert \
    --certPassword $CERT_PASSWORD_OR_PIN \
    --keystorePassword $KS_PASSWORD_OR_PIN \
    --pkcs11CfgFile $PATH_OF_PKCS11_CFG_FILE
```
