cmake_minimum_required(VERSION 3.22.1)

project("adb_pairing")

# 1. Find the library
find_package(boringssl REQUIRED CONFIG)

add_library(adb SHARED
        adb_pairing.cpp)

# 2. Link against 'crypto_static' instead of 'crypto'
target_link_libraries(adb
        boringssl::crypto_static
        log)