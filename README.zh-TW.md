MIFARE Classic Tool (MCT)
=========================

一款用於讀取、寫入、分析 MIFARE Classic RFID 標籤等操作的 Android NFC 應用程式。

<a href="https://play.google.com/store/apps/details?id=de.syss.MifareClassicTool"><img src="metadata/common/assets/google-play-badge.png" alt="從 Play 商店下載" height="80"></a>
<a href="https://f-droid.org/packages/de.syss.MifareClassicTool/"><img src="metadata/common/assets/fdroid-badge.png" alt="從 F-Droid 下載" height="80"></a>
<a href="https://www.icaria.de/mct/releases/"><img src="metadata/common/assets/direct-apk-download-badge.png" alt="直接下載 APK" height="80"></a>

以其他語言閱讀本文：

* [English](README.md)
* [简体中文](README.zh-CN.md)

實用連結：

* [Google Play 上的 MIFARE Classic Tool (贊助版)](https://play.google.com/store/apps/details?id=de.syss.MifareClassicToolDonate)
* [擷圖](https://www.icaria.de/mct/screenshots/latest/)
* [說明與資訊/使用手冊](https://www.icaria.de/mct/help-and-info/)
* [其他資源](https://www.icaria.de/mct/) (文件、APK 檔案等)
* [Proxmark 論壇討論串](http://www.proxmark.org/forum/viewtopic.php?id=1535)
* [透過 Paypal 贊助](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=24ET8A36XLMNW) [![贊助](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=24ET8A36XLMNW)

功能特色
--------

* 讀取 MIFARE Classic 標籤
* 儲存、編輯與分享讀取到的標籤資料
* 寫入 MIFARE Classic 標籤 (逐區塊寫入)
* 複製 MIFARE Classic 標籤
  (將標籤的傾印寫入另一張標籤；以傾印方式寫入)
* 基於字典攻擊的金鑰管理
  (將已知的金鑰寫入檔案 (字典))。
  MCT 會嘗試以這些金鑰對所有磁區進行驗證，並盡可能讀取更多資料。
  請參閱[快速入門](#快速入門)章節。
* 將標籤格式化為出廠/初始狀態
* 對特殊 MIFARE Classic 標籤寫入製造商區塊 (區塊 0)
* 使用外接 NFC 讀卡機 (例如 ACR 122U)
  (請參閱[說明與資訊](https://publications.icaria.de/mct/help-and-info/#external_nfc)
  以取得更多資訊。)
* 建立、編輯、儲存與分享金鑰檔案 (字典)
* 解碼與編碼 MIFARE Classic 數值區塊
* 解碼與編碼 MIFARE Classic 存取條件
* 比較傾印檔 (差異比對工具)
* 顯示標籤一般資訊
* 以醒目標示的十六進位格式顯示標籤資料
* 以 7-Bit US-ASCII 格式顯示標籤資料
* 以表格形式顯示 MIFARE Classic 存取條件
* 以整數顯示 MIFARE Classic 數值區塊
* 計算 BCC (Block Check Character)
* 快速 UID 複製功能
* 匯入/匯出/轉換檔案
* 內建離線說明與資訊
* 這是自由軟體 (開放原始碼) ;)

一般資訊
--------

此工具提供多種與 MIFARE Classic RFID 標籤互動的功能 (且僅限於此)。
適用於至少對 MIFARE Classic 技術有基本認識的使用者。
此外，由於所有資料的輸入與輸出皆採用十六進位格式，
使用者也需要具備十六進位數字系統的基礎知識。

幾項重要事項：

* 此工具提供的功能非常基礎。沒有像是透過精美圖形介面
  將 URL 儲存到 RFID 標籤之類的進階功能。若要在標籤上儲存資料，
  必須直接輸入原始的十六進位資料。
* 此 App **無法破解**任何 MIFARE Classic 金鑰。若要讀取/寫入
  RFID 標籤，必須先取得該標籤的金鑰。
  如需更多資訊，請參閱[快速入門](#快速入門)章節。
* 此應用程式**不會提供「暴力破解」**功能，
  因為在此通訊協定下速度過慢。
* 請注意！解除安裝此 App 會永久刪除其儲存的所有檔案
  (傾印/金鑰)。
* **原版** MIFARE Classic 標籤的第一個磁區第一個區塊
  為**唯讀**，無法寫入。但有一些**特殊**的 MIFARE Classic
  標籤可用一般寫入指令寫入製造商區塊 (通常稱為
  「Magic Tag gen2」或「CUID」)。此 App 能寫入此類標籤，
  因此可製作完全正確的複製品。「FUID」和「UFUID」標籤理論上也能使用，
  但尚未經過測試。然而，並非所有特殊標籤都能使用。部分標籤需要
  **特殊指令序列** (由於 Android NFC API 的限制而無法傳送)
  才能進入可寫入製造商區塊的狀態。
  這類標籤通常稱為「gen1」、「gen1a」或「UID」。
  購買特殊標籤時請特別留意！
  更多有關 Magic Card 的資訊請參閱
  [此頁面](https://github.com/RfidResearchGroup/proxmark3/blob/master/doc/magic_cards_notes.md)。
  此外，請確保 BCC 值 (可使用「BCC 計算器」)、
  SAK 和 ATQA 值正確。若只需複製 UID，
  請使用「複製 UID 工具」。
* 此 App 在某些裝置上**無法運作**，因為其硬體
  (NFC 控制器) 不支援 MIFARE Classic
  ([瞭解更多](https://github.com/ikarus23/MifareClassicTool/issues/1))。
  **不相容裝置清單請參閱
  [此頁面](https://github.com/ikarus23/MifareClassicTool/blob/master/INCOMPATIBLE_DEVICES.md)**。

如需更多有關 MIFARE Classic 的資訊，請參閱
[維基百科](https://en.wikipedia.org/wiki/MIFARE)、
[Google 搜尋](https://www.google.com/search?q="mifare+classic")
或閱讀 NXP 的 [MIFARE Classic (1k) 資料表](https://www.nxp.com/docs/en/data-sheet/MF1S50YYX_V1.pdf)(PDF)。

快速入門
--------

首先，需要取得目標標籤的金鑰。
由於 MIFARE Classic 存在一些弱點，可使用
[Proxmark3](http://www.proxmark.org/) 等工具或
一般 RFID 讀卡機搭配特殊軟體
([mfcuk](https://github.com/nfc-tools/mfcuk)、
[mfoc](https://github.com/nfc-tools/mfoc))
來取得標籤的所有金鑰 (A 和 B)。

此應用程式內建名為
*std.keys*、*extended-std.keys* 和 *hotel-std.keys* 的標準金鑰檔案，
包含常見的金鑰以及從 Google 搜尋取得的部分標準金鑰。
可嘗試使用主選單的「讀取標籤」搭配這些金鑰檔案來讀取標籤。
對這些金鑰檔案所做的變更不會保留，請自行建立金鑰檔案來管理金鑰。

取得金鑰後，可將金鑰放入純文字檔案中 (每行一組金鑰)。
可在電腦上完成此步驟後，透過 MCT 的匯入/匯出工具匯入檔案，
或從主選單透過「編輯/新增金鑰檔案」建立新的金鑰檔案。
金鑰檔案準備就緒後，即可使用主選單的「讀取標籤」
來讀取標籤。

金鑰檔案機制的優勢：

* **不需要知道哪組金鑰對應哪個磁區。**
  應用程式會嘗試以金鑰檔案 (字典) 中所有金鑰進行驗證。
* **不需要知道所有金鑰。**
  如果在金鑰檔案 (字典) 中找不到特定磁區的金鑰 A 或金鑰 B，
  應用程式會略過該磁區的讀取。

這種基於字典攻擊的對應流程
(金鑰 &lt;-&gt; 磁區) 能以已知的金鑰盡可能讀取最多資料！

授權條款
--------

此應用程式最初由 Gerhard Klostermeier 與 SySS GmbH 及 Aalen
大學 ([www.htw-aalen.de](http://www.htw-aalen.de/)) 於 2012/2013 年合作開發。
這是自由軟體，採用
[GNU General Public License v3.0 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.txt) 授權

應用程式中使用的圖示：

* Logo：[Beneke Traub](http://www.beneketraub.com/)
  ([Creative Commons 4.0](http://creativecommons.org/licenses/by-nc-sa/4.0/))
* Oxygen Icons：[www.oxygen-icons.org](http://www.oxygen-icons.org/)
  ([GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl.html))
* RFID Tag：[www.nfc-tag.de](http://www.nfc-tag.de/)
  ([Creative Commons 3.0](http://creativecommons.org/licenses/by/3.0/))

MIFARE® 為 NXP Semiconductors 的註冊商標。
