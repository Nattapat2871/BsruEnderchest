# 📦 BsruEnderchest

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Repo stars](https://img.shields.io/github/stars/Nattapat2871/BsruEnderchest?style=flat-square)](https://github.com/Nattapat2871/BsruEnderchest/stargazers)
![Visitor Badge](https://api.visitorbadge.io/api/VisitorHit?user=Nattapat2871&repo=BsruEnderchest&countColor=%237B1E7A&style=flat-square)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Nattapat2871)

</div>

<p align= "center">
      <a href="/README.md">English</a> | <b>ภาษาไทย</b>
</p>

BsruEnderchest คือปลั๊กอิน Ender Chest ประสิทธิภาพสูงระดับพรีเมียมสำหรับเซิร์ฟเวอร์ Spigot, Paper และ **Folia** ที่จะมาแทนที่ Ender Chest เดิมๆ ของคุณด้วยระบบที่เหนือกว่า ทั้งขนาดที่ใหญ่ขึ้น, ระบบหลายหน้า, การซิงค์ข้อมูลข้ามเซิร์ฟเวอร์ผ่าน MySQL, และชุดเครื่องมือสำหรับแอดมินที่ทรงพลัง ทั้งหมดนี้ถูกออกแบบมาให้ทำงานได้อย่างราบรื่นและไม่ทำให้เซิร์ฟเวอร์ค้าง

## 🌟 การรองรับเวอร์ชันใหม่
- **Minecraft 1.20, 1.21 และ 26.1.x (2026 Drops)**
- **Java 21 ถึง Java 25+**
- **รองรับ Folia แบบ Native**
- **ผสานการทำงานกับ Adventure Component API**

## ✨ คุณสมบัติเด่น

- **ขยายช่องเก็บของ:** เพิ่มขนาด Ender Chest เป็น 6 แถวเต็ม (54 ช่อง)
- **ระบบหลายหน้า:** สามารถให้สิทธิ์ผู้เล่นในการเข้าถึงช่องเก็บของหลายหน้าได้ตาม Permission
- **รองรับ Folia:** ปลั๊กอินถูกออกแบบมาให้ทำงานได้ทั้งบน Paper/Spigot และ Folia ในไฟล์เดียวกัน
- **ระบบจัดเก็บ 2 โหมด:**
  - **ระบบไฟล์:** ใช้งานง่าย ไม่ต้องตั้งค่าเพิ่มเติม เก็บข้อมูลในเครื่อง (`playerdata/*.yml`)
  - **ระบบ MySQL:** เก็บข้อมูลไว้ที่ฐานข้อมูลส่วนกลาง ทำให้สามารถซิงค์ข้อมูล Ender Chest ข้ามเซิร์ฟเวอร์ได้
- **การทำงานแบบ Asynchronous:** การโหลดและบันทึกข้อมูลทั้งหมดจะทำงานอยู่เบื้องหลัง (Background Thread) เพื่อป้องกันปัญหาเซิร์ฟเวอร์หน่วงหรือค้าง
- **การบีบอัดข้อมูล:** ข้อมูลไอเทมจะถูกบีบอัดด้วย GZIP ก่อนบันทึกลงฐานข้อมูล ช่วยลดขนาดพื้นที่จัดเก็บและลดปริมาณการรับส่งข้อมูล
- **ระบบย้ายข้อมูลอัจฉริยะ:** หากผู้เล่นได้รับการอัปเกรดสิทธิ์ ปลั๊กอินจะย้ายไอเทมทั้งหมดไปยังที่ใหม่ให้อัตโนมัติ ป้องกันของหาย 100%
- **ระบบแจ้งเตือนแอดมิน:** เมื่อเกิดปัญหาการเชื่อมต่อฐานข้อมูล จะมีการแจ้งเตือนไปที่แอดมินที่ออนไลน์อยู่ทันที
- **ทำงานร่วมกับ WorldGuard:** ป้องกันผู้เล่นทิ้งของออกจากกล่องในพื้นที่ที่ตั้งค่า `item-drop` เป็น deny
- **ปรับแต่งได้เต็มรูปแบบ:** สามารถปรับแต่งหน้าตา UI, ข้อความ, ชื่อกล่อง, และเสียงประกอบต่างๆ ได้อย่างอิสระผ่านไฟล์ `config.yml`

## 🚀 การติดตั้ง

1.  ดาวน์โหลดไฟล์ `.jar` เวอร์ชันล่าสุดจากหน้า [Releases](https://github.com/Nattapat2871/BsruEnderchest/releases)
2.  นำไฟล์ `BsruEnderchest-X.X.jar` ไปไว้ในโฟลเดอร์ `/plugins` ของเซิร์ฟเวอร์
3.  **(แนะนำ)** เพื่อการทำงานเต็มประสิทธิภาพ ควรติดตั้ง **[LuckPerms](https://luckperms.net/download)** และ **[WorldGuard](https://dev.bukkit.org/projects/worldguard)**
4.  เปิดหรือรีสตาร์ทเซิร์ฟเวอร์ เพื่อให้ปลั๊กอินสร้างไฟล์ตั้งค่าเริ่มต้น
5.  แก้ไข `config.yml` ตามต้องการ (เช่น เปิดใช้งานฐานข้อมูล)
6.  เปิดเซิร์ฟเวอร์อีกครั้ง หรือใช้คำสั่ง `/bsruenderchest reload`
7.  ตั้งค่า Permission ให้กับผู้เล่นและยศต่างๆ

## ⚙️ คำสั่ง

| คำสั่ง | คำสั่งย่อ | Permission | คำอธิบาย |
| :--- | :--- | :--- | :--- |
| `/enderchest` | `/ec` | `bsruenderchest.command.use` | เปิด Ender Chest ส่วนตัวของคุณ |
| `/bsruenderchest`| `/bec`| (ไม่มี) | แสดงข้อมูลปลั๊กอิน |
| `/bsruenderchest reload`| `/bec reload`| `bsruenderchest.admin.reload` | รีโหลดไฟล์ตั้งค่าของปลั๊กอิน |
| `/bsruenderchest chestsee <player>`|`/bec chestsee <player>`| `bsruenderchest.admin.chestsee`| เปิด Ender Chest ของผู้เล่นอื่น |

## 🔑 สิทธิ์ (Permission)

| Permission | คำอธิบาย | ค่าเริ่มต้น |
| :--- | :--- | :--- |
| `bsruenderchest.use` | สิทธิ์ในการเปิด Ender Chest โดยการคลิกที่บล็อก | `true` (ทุกคน) |
| `bsruenderchest.command.use` | สิทธิ์ในการใช้คำสั่ง `/enderchest` | `true` (ทุกคน) |
| `bsruenderchest.plus.<ตัวเลข>`| ให้สิทธิ์ในการเข้าถึง Ender Chest จำนวน `<ตัวเลข>` หน้า (สูงสุด 10) | `op` |
| `bsruenderchest.admin.reload`| อนุญาตให้ใช้คำสั่ง `/bec reload` | `op` |
| `bsruenderchest.admin.chestsee`| อนุญาตให้ใช้คำสั่ง `/bec chestsee <player>` | `op` |
| `bsruenderchest.admin.notify`| ได้รับการแจ้งเตือนในเกมเมื่อฐานข้อมูลมีปัญหา | `op` |

## 📝 การตั้งค่า (`config.yml`)

```yml
# BsruEnderchest Final Version Configuration

# ---------------------------------
# Database Settings
# ---------------------------------
# ตั้งค่าเป็น true เพื่อใช้งานระบบฐานข้อมูล MySQL (สำหรับเก็บข้อมูลข้ามเซิร์ฟ)
# ถ้าเป็น false ปลั๊กอินจะเก็บข้อมูลเป็นไฟล์ในโฟลเดอร์ playerdata เหมือนเดิม
database:
  enable: false
  host: "localhost"
  port: 3306
  database: "bsru_enderchest"
  username: "user"
  password: "password"

# ---------------------------------
# Inventory Titles
# ---------------------------------
# รูปแบบของชื่อ Inventory. {current_page}, {max_pages}
inventory-title-format: "&5&lEnder Chest &8(&fหน้า {current_page}/{max_pages}&8)"
# ชื่อ Inventory สำหรับผู้เล่นที่มีแค่ permission .use (โหมด 6 แถวเต็ม)
single-page-title: "&5&lEnder Chest (6 Rows)"
# รูปแบบชื่อ Inventory "ตอนที่แอดมินส่อง". {player_name}, {current_page}, {max_pages}
admin-inventory-title-format: "&c&lAdmin View: {player_name} &8(&fหน้า {current_page}/{max_pages}&8)"
# รูปแบบชื่อ Inventory "ตอนที่แอดมินส่อง" ผู้เล่นที่มีแค่หน้าเดียว
admin-single-page-title: "&c&lAdmin View: &e{player_name} &8(6 Rows)"

# ---------------------------------
# Messages
# ---------------------------------
# ข้อความเมื่อไม่มีสิทธิ์ใช้คำสั่ง
no-permission-command-message: "&cคุณไม่มีสิทธิ์ใช้คำสั่งนี้"
# ข้อความแจ้งเตือน "ผู้เล่น" เมื่อฐานข้อมูลมีปัญหา
player-database-error-message: "&c[Enderchest] ระบบมีปัญหาชั่วคราว กรุณาลองอีกครั้ง"
# ข้อความแจ้งเตือน "แอดมิน"
admin-database-error-message: "&c&l[BSRU Enderchest] &cCRITICAL: ไม่สามารถเชื่อมต่อฐานข้อมูลได้! กรุณาตรวจสอบ Console ด่วน"
# ข้อความที่จะแสดงเมื่อผู้เล่นพยายามทิ้งของในเขตของ WorldGuard ที่ห้ามไว้
worldguard-drop-deny-message: "&cคุณไม่สามารถทิ้งของในบริเวณนี้ได้"

# ---------------------------------
# Control Panel Items (สำหรับโหมดหลายหน้า)
# ---------------------------------
control-panel:
  previous-page:
    enabled:
      material: "MAGMA_CREAM"
      name: "&a&lหน้าก่อนหน้า"
      lore:
        - "&7คลิกเพื่อไปยังหน้า &e{previous_page}"
    disabled:
      material: "BARRIER"
      name: "&c&lหน้าแรก"
      lore:
        - "&7คุณอยู่ที่หน้าแรกแล้ว"
  next-page:
    enabled:
      material: "ARROW"
      name: "&a&lหน้าถัดไป"
      lore:
        - "&7คลิกเพื่อไปยังหน้า &e{next_page}"
    disabled:
      material: "BARRIER"
      name: "&c&lหน้าสุดท้าย"
      lore:
        - "&7คุณอยู่ที่หน้าสุดท้ายแล้ว"
  filler:
    material: "GRAY_STAINED_GLASS_PANE"
    name: " "

# ---------------------------------
# Sounds
# ---------------------------------
# name: ชื่อเสียงจาก Minecraft (เช่น ui.button.click)
# volume: ระดับความดัง (เช่น 1.0)
# pitch: ระดับความสูง-ต่ำของเสียง (เช่น 1.0)
sounds:
  navigate:
    name: "ui.button.click"
    volume: 0.8
    pitch: 1.2
  fail:
    name: "block.anvil.place"
    volume: 1.0
    pitch: 1.0
  worldguard-deny:
    name: "entity.villager.no"
    volume: 1.0
    pitch: 0.8
```

## 📄 License

โปรเจกต์นี้อยู่ภายใต้ลิขสิทธิ์ MIT ดูรายละเอียดเพิ่มเติมได้ที่ไฟล์ [LICENSE](https://github.com/Nattapat2871/BsruEnderchest/blob/main/LICENSE)