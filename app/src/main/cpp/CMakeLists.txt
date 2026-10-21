cmake_minimum_required(VERSION 3.22.1)
project("cheat")

add_library(cheat SHARED native-lib.cpp)
add_executable(ff_daemon daemon.cpp)

find_library(log-lib log)
target_link_libraries(cheat ${log-lib} android)
target_link_libraries(ff_daemon ${log-lib} android)
