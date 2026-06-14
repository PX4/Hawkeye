cmake_minimum_required(VERSION 3.22)
project(hawkeye C)

# Tell Raylib to target Android with OpenGL ES 3.0
set(PLATFORM "Android" CACHE STRING "" FORCE)
set(GRAPHICS "GRAPHICS_API_OPENGL_ES3" CACHE STRING "" FORCE)

# Fetch Raylib 5.5 — same version as the desktop project
include(FetchContent)
FetchContent_Declare(
    raylib
    GIT_REPOSITORY https://github.com/raysan5/raylib.git
    GIT_TAG        5.5
    GIT_SHALLOW    TRUE
)
FetchContent_MakeAvailable(raylib)

# Hawkeye rendering source subset — no MAVLink/HUD; ULog replay is included so
# the app can play back .ulg files delivered via VIEW/SEND intents.
# CMakeLists.txt lives at android/app/src/main/cpp/ — five levels up is the repo root
set(HAWKEYE_SRC "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../src")

# MAVLink c_library_v2 (header-only, git submodule) -- repo root is five levels up.
set(MAVLINK_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../lib/c_library_v2")

add_library(hawkeye SHARED
    android_main.c
    replay_jni.c
    swarm_jni.c
    ${HAWKEYE_SRC}/scene.c
    ${HAWKEYE_SRC}/vehicle.c
    ${HAWKEYE_SRC}/theme.c
    ${HAWKEYE_SRC}/ortho_panel.c
    ${HAWKEYE_SRC}/data_source_ulog.c
    ${HAWKEYE_SRC}/ulog_parser.c
    ${HAWKEYE_SRC}/ulog_replay.c
    ${HAWKEYE_SRC}/ulog_replay_apply.c

    # HUD subsystem — shared with desktop and WASM builds. Mirrors wasm/CMakeLists.txt.
    ${HAWKEYE_SRC}/hud.c
    ${HAWKEYE_SRC}/hud_help.c
    ${HAWKEYE_SRC}/hud_instruments.c
    ${HAWKEYE_SRC}/hud_transport.c
    ${HAWKEYE_SRC}/hud_telemetry.c
    ${HAWKEYE_SRC}/hud_annunciators.c
    ${HAWKEYE_SRC}/tactical_hud.c
    ${HAWKEYE_SRC}/debug_panel.c
    ${HAWKEYE_SRC}/replay_trail.c
    ${HAWKEYE_SRC}/replay_markers.c
    ${HAWKEYE_SRC}/replay_conflict.c
    ${HAWKEYE_SRC}/ui_marker_input.c
    ${HAWKEYE_SRC}/mavlink_receiver.c
    ${HAWKEYE_SRC}/data_source_mavlink.c
)

target_include_directories(hawkeye PRIVATE
    ${HAWKEYE_SRC}
    ${MAVLINK_DIR}
    ${MAVLINK_DIR}/common
    ${ANDROID_NDK}/sources/android/native_app_glue
)

target_compile_definitions(hawkeye PRIVATE MAVLINK_COMM_NUM_BUFFERS=16)

target_link_libraries(hawkeye
    raylib
    android
    log
    EGL
    GLESv3
)

# ANativeActivity_onCreate is called by the Android Java runtime via JNI, not by any
# C code — so the linker would silently strip it from the shared library without this.
target_link_options(hawkeye PRIVATE
    "-Wl,-u,ANativeActivity_onCreate"
)
