import os
import subprocess
import zipfile
import shutil

PROJECT_DIR = r"C:\Users\SKSAL\.gemini\antigravity\scratch\MiuiGestureWidth"
ANDROID_JAR = r"C:\Users\SKSAL\AppData\Local\Android\Sdk\platforms\android-34\android.jar"
BUILD_TOOLS = r"C:\Users\SKSAL\AppData\Local\Android\Sdk\build-tools\34.0.0"
JAVA_BIN = r"C:\Program Files\Android\Android Studio\jbr\bin"
JAVA_HOME = r"C:\Program Files\Android\Android Studio\jbr"
os.environ["JAVA_HOME"] = JAVA_HOME
os.environ["PATH"] = JAVA_BIN + os.pathsep + os.environ.get("PATH", "")

AAPT = os.path.join(BUILD_TOOLS, "aapt.exe")
D8 = os.path.join(BUILD_TOOLS, "d8.bat")
ZIPALIGN = os.path.join(BUILD_TOOLS, "zipalign.exe")
APKSIGNER = os.path.join(BUILD_TOOLS, "apksigner.bat")
JAVAC = os.path.join(JAVA_BIN, "javac.exe")
KEYTOOL = os.path.join(JAVA_BIN, "keytool.exe")
JAR = os.path.join(JAVA_BIN, "jar.exe")

bin_dir = os.path.join(PROJECT_DIR, "bin")
stubs_bin = os.path.join(bin_dir, "stubs_classes")
classes_dir = os.path.join(bin_dir, "classes")
gen_dir = os.path.join(PROJECT_DIR, "gen")

shutil.rmtree(bin_dir, ignore_errors=True)
shutil.rmtree(gen_dir, ignore_errors=True)
os.makedirs(classes_dir, exist_ok=True)
os.makedirs(stubs_bin, exist_ok=True)
os.makedirs(gen_dir, exist_ok=True)

# Step 0: Compile Stubs into xposed-stubs.jar
print("--- Compiling Xposed Stubs ---")
stub_files = []
for root, _, files in os.walk(os.path.join(PROJECT_DIR, "stubs")):
    for f in files:
        if f.endswith(".java"):
            stub_files.append(os.path.join(root, f))

cmd_stubs = [
    JAVAC, "-d", stubs_bin,
    "-source", "1.8", "-target", "1.8",
    "-cp", ANDROID_JAR
] + stub_files
subprocess.check_call(cmd_stubs)

stubs_jar = os.path.join(bin_dir, "xposed-stubs.jar")
cmd_jar = [JAR, "cf", stubs_jar, "-C", stubs_bin, "."]
subprocess.check_call(cmd_jar)

# Step 1: Generate R.java
print("--- Generating R.java ---")
cmd_r = [
    AAPT, "package", "-m",
    "-J", gen_dir,
    "-M", os.path.join(PROJECT_DIR, "AndroidManifest.xml"),
    "-S", os.path.join(PROJECT_DIR, "res"),
    "-I", ANDROID_JAR
]
subprocess.check_call(cmd_r)

# Step 2: Compile App Java sources (src + gen)
print("--- Compiling App Java Sources ---")
app_java_files = []
for root, _, files in os.walk(os.path.join(PROJECT_DIR, "src")):
    for f in files:
        if f.endswith(".java"):
            app_java_files.append(os.path.join(root, f))
for root, _, files in os.walk(gen_dir):
    for f in files:
        if f.endswith(".java"):
            app_java_files.append(os.path.join(root, f))

cmd_javac = [
    JAVAC,
    "-d", classes_dir,
    "-cp", f"{ANDROID_JAR};{stubs_jar}",
    "-source", "1.8",
    "-target", "1.8"
] + app_java_files
subprocess.check_call(cmd_javac)

# Step 3: Run D8
print("--- Packaging classes to JAR and running D8 dexer ---")
app_classes_jar = os.path.join(bin_dir, "app-classes.jar")
subprocess.check_call(["jar", "cf", app_classes_jar, "-C", classes_dir, "."])
cmd_d8 = [
    D8,
    "--output", bin_dir,
    "--lib", ANDROID_JAR,
    "--classpath", stubs_jar,
    "--min-api", "26",
    app_classes_jar
]
subprocess.check_call(cmd_d8, shell=True)

# Step 4: Package base APK with AAPT
print("--- Packaging Base APK ---")
unaligned_apk = os.path.join(bin_dir, "unaligned.apk")
cmd_aapt = [
    AAPT, "package", "-f",
    "-M", os.path.join(PROJECT_DIR, "AndroidManifest.xml"),
    "-S", os.path.join(PROJECT_DIR, "res"),
    "-A", os.path.join(PROJECT_DIR, "assets"),
    "-I", ANDROID_JAR,
    "-F", unaligned_apk
]
subprocess.check_call(cmd_aapt)

# Step 5: Add classes.dex into APK
print("--- Adding classes.dex to APK ---")
dex_file = os.path.join(bin_dir, "classes.dex")
with zipfile.ZipFile(unaligned_apk, 'a') as z:
    z.write(dex_file, "classes.dex")

# Step 6: Zipalign
print("--- Running Zipalign ---")
aligned_apk = os.path.join(bin_dir, "aligned.apk")
subprocess.check_call([ZIPALIGN, "-p", "-f", "4", unaligned_apk, aligned_apk])

# Step 7: Sign APK
print("--- Signing APK ---")
keystore = os.path.join(PROJECT_DIR, "debug.keystore")
if not os.path.exists(keystore):
    cmd_keytool = [
        KEYTOOL, "-genkeypair",
        "-keystore", keystore,
        "-storepass", "android",
        "-alias", "androiddebugkey",
        "-keypass", "android",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US"
    ]
    subprocess.check_call(cmd_keytool)

signed_apk = os.path.join(PROJECT_DIR, "MiuiGestureWidth.apk")
if os.path.exists(signed_apk):
    os.remove(signed_apk)

cmd_sign = [
    APKSIGNER, "sign",
    "--ks", keystore,
    "--ks-pass", "pass:android",
    "--ks-key-alias", "androiddebugkey",
    "--key-pass", "pass:android",
    "--out", signed_apk,
    aligned_apk
]
subprocess.check_call(cmd_sign, shell=True)

print(f"\n SUCCESS! Created signed APK: {signed_apk}")
print(f"APK Size: {os.path.getsize(signed_apk)} bytes")

