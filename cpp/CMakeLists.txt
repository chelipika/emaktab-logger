cmake_minimum_required(VERSION 3.10)
project(CurlJsonExample)

find_package(CURL REQUIRED)
# 1. Add this line to find the JSON library
find_package(nlohmann_json CONFIG REQUIRED)

add_executable(my_app main.cpp)

set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libgcc -static-libstdc++ -static")

# 2. Add nlohmann_json::nlohmann_json to the link list
target_link_libraries(my_app PRIVATE 
    CURL::libcurl 
    nlohmann_json::nlohmann_json
)
