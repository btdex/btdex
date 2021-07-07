;Include Modern UI
!include "MUI2.nsh"

;Basic configuration
Name "BTDEX"
OutFile "btdex-installer-win_x64.exe"
Unicode True
;Default installation folder
InstallDir "$PROGRAMFILES64\btdex"
;RequestExecutionLevel none
!define MUI_ABORTWARNING

;Pages
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES  


!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT "Launch BTDEX"
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchApplication"
!insertmacro MUI_PAGE_FINISH

!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchApplication"

Function LaunchApplication
    ExecShell "" "$INSTDIR\btdex.exe"
FunctionEnd


;Languages
;!insertmacro MUI_LANGUAGE "English"

;Installer Sections
Section "" SecExample
  SetOutPath "$INSTDIR"
  FILE ../LICENSE
  FILE ../build/launch4j/btdex.exe
  FILE /r ../dist/jdk/zulu8.54.0.21-ca-jdk8.0.292-win_x64/jre

  WriteUninstaller $INSTDIR\uninstall.exe
SectionEnd

; The uninstall section
Section "Uninstall"

Delete $INSTDIR\uninstall.exe
Delete $INSTDIR\btdex.exe
RMDir $INSTDIR

SectionEnd

