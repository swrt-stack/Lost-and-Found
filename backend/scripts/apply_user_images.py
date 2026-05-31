# -*- coding: utf-8 -*-
"""将用户提供的参考图复制到 uploads/demo 并转为 JPEG。"""
from pathlib import Path
from PIL import Image

ASSETS = Path(r"C:\Users\asus\.cursor\projects\d-GraduationDesign\assets")
OUT = Path(__file__).resolve().parent.parent / "uploads" / "demo"

MAPPING = {
    "lost-01-earbuds.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-d43cc013-0ff8-44d1-92bf-ae1b7ef8023a.png",
    "lost-02-card.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-ab2bbb49-799b-4e1c-aca6-dfec34ca9fe6.png",
    "lost-03-book.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-3d1fbb57-576d-4346-9f2e-05743fcefa62.png",
    "lost-04-thermos.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-4b42af1e-dc14-4b34-9bb0-4b2843235197.png",
    "lost-06-phone.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-a57c7cf0-1af8-4000-a761-c0ca7809cf0e.png",
    "lost-07-keyboard.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-7c3394d4-7969-4f09-88ab-2849f0b05004.png",
    "lost-09-umbrella.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-0e21c267-c9b8-40b0-8de1-869c9baee75d.png",
    "lost-10-glasses.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-b89d41b1-6519-4b51-a84b-f15190278c09.png",
    "found-01-headphone.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-8b5aa9e6-65bf-4031-b05d-118d81ff10d1.png",
    "found-02-student-card.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-423f0f51-c8a7-4622-b753-1a8f3da32952.png",
    "found-03-textbook.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-db20218e-ab10-40ae-b536-913783227594.png",
    "found-05-bag.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-816eb271-f26b-4aec-8c68-30f8cff43969.png",
    "found-06-powerbank.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-a09fdf14-6610-472c-89fa-345891397b88.png",
    "found-07-mouse.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-283d30c2-b0c5-41f0-8b4a-4077bfb03b83.png",
    "found-08-bankcard.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-86669676-66b8-4147-9bff-c85f415ba23e.png",
    "found-09-hat.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-2b10ccaa-5117-4a62-95fa-d1e74a571158.png",
    "found-10-keys.jpg": "c__Users_asus_AppData_Roaming_Cursor_User_workspaceStorage_0c20ede6f557460a23e81dbfa714b6ef_images_image-0c193710-16e8-49c9-8a52-933bdc833c95.png",
}

def convert(src: Path, dst: Path) -> None:
    img = Image.open(src)
    if img.mode in ("RGBA", "P"):
        img = img.convert("RGB")
    elif img.mode != "RGB":
        img = img.convert("RGB")
    max_side = 1200
    w, h = img.size
    if max(w, h) > max_side:
        ratio = max_side / max(w, h)
        img = img.resize((int(w * ratio), int(h * ratio)), Image.Resampling.LANCZOS)
    dst.parent.mkdir(parents=True, exist_ok=True)
    img.save(dst, "JPEG", quality=88, optimize=True)

def main() -> None:
    for name, src_name in MAPPING.items():
        src = ASSETS / src_name
        if not src.exists():
            raise FileNotFoundError(src)
        dst = OUT / name
        convert(src, dst)
        print(f"OK {name} <- {src_name} ({dst.stat().st_size} bytes)")

if __name__ == "__main__":
    main()
