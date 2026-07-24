# MaterialTV - AI Agent Rules

*Kural: Projede herhangi bir geliştirme, hata ayıklama veya mimari karar almadan önce daima `docs/memory-bank.md` ve `docs/todo.md` dosyalarını oku. Bu dosyalar projenin tek gerçeklik kaynağıdır (single source of truth). Aksi belirtilmedikçe sadece oradaki kurallara, tasarım prensiplerine ve yol haritasına uy.*

*Kural (Derleme): Uygulamanın derlenmesi istendiğinde sadece APK oluşturmakla (assemble) kalma. Daima bağlı bir cihaza veya emülatöre kurmak için `./gradlew installDebug` komutunu kullan. APK dosyaları ortada bırakılmamalı, cihaza yüklenmelidir.*

*Kural (İmzalama / Keystore): İmzalı release derlemelerinde daima resmi imza anahtarı olan `/home/hasan/ANDROİD STUDİO KEYS DO NOT KİLL/key.jks` (Alias: `key0`) kullanılmalıdır. Proje kökündeki `gradle.properties` bu konumu işaret etmelidir.*

*Kural (Güncelleme / Update Manager & GitHub Release): Uygulama içi güncelleme mekanizması `https://api.github.com/repos/hasan-ege/MaterialTV/releases/latest` API adresi üzerinden çalışır. GitHub üzerinde yeni sürüm yayınlandığında Release APK varlığı `MaterialTV-{sürüm}.apk` (Örn: `MaterialTV-3.0.apk` / `MaterialTV-v3.0.apk`) formatında yüklenmelidir.*
