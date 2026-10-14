# 发布新版本

更新 `AndroidManifest.xml` 和 `app/build.gradle` 中的 `versionName`，并递增 `versionCode`；两处必须一致。提交并推送到 `main` 后，GitHub Actions 会使用原有签名构建 APK，自动创建 `v版本号` 的 GitHub Release，并附上 APK 和 `SHA256SUMS.txt`。已发布的版本会跳过，重复运行不会覆盖附件。

可在 `docs/releases/版本号.md` 填写本次更新说明（例如 `docs/releases/1.26.md`）；未提供时使用 GitHub 自动生成的说明。其他分支和 Pull Request 只检查构建。签名密钥继续使用 `apk-signing` 环境中的 `ANDROID_SIGNING_KEY_BASE64`。
