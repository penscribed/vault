# Backup JSON is (de)serialised by Gson via reflection
-keep class com.kingsman.vault.data.** { *; }
# SQLCipher native bindings
-keep class net.zetetic.database.** { *; }
