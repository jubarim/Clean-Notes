print_red(){
    printf "\e[1;31m$1\e[0m"
}
print_green(){
    printf "\e[1;32m$1\e[0m"
}
print_yellow(){
    printf "\e[1;33m$1\e[0m"
}
print_blue(){
    printf "\e[1;34m$1\e[0m"
}

print_blue "\n\nStarting Firestore local emulator..."
firebase emulators:exec --only firestore "./ui_and_unit_tests.sh"
