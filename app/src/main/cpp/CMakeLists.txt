cmake_minimum_required(VERSION 3.22.1)
project(multi2bypass CXX)

add_library(multi2bypass SHARED bypass.cpp)

find_library(log_lib log)
target_link_libraries(multi2bypass ${log_lib})
