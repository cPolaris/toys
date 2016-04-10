#!/bin/bash
XCODE_BRIGHT='Default.dvtcolortheme'
XCODE_DARK='Dusk.dvtcolortheme'
VIM_BRIGHT='colorscheme default'
VIM_DARK='colorscheme molokai'
SUBLIME_BRIGHT='Packages/Theme - Spacegray/base16-ocean.light.tmTheme'
SUBLIME_DARK='Packages/Theme - Spacegray/base16-eighties.dark.tmTheme'
SUBLIME_BRIGHT_MD='Packages/MarkdownEditing/MarkdownEditor.tmTheme'
SUBLIME_DARK_MD='Packages/MarkdownEditing/MarkdownEditor-Dark.tmTheme'

xcodeTheme=$XCODE_BRIGHT
vimTheme=$VIM_BRIGHT
vimOldTheme=$VIM_DARK
sublimeTheme=$SUBLIME_BRIGHT
sublimeThemeMd=$SUBLIME_BRIGHT_MD

if [ "$1" == "D" ]; then
    xcodeTheme=$XCODE_DARK
    vimTheme=$VIM_DARK
    vimOldTheme=$VIM_BRIGHT
    sublimeTheme=$SUBLIME_DARK
    sublimeThemeMd=$SUBLIME_DARK_MD
fi

# Xcode
# cd /Users/cPolaris/Library/Preferences
# defaults write com.apple.dt.Xcode DVTFontAndColorCurrentTheme $xcodeTheme

# Vim
# cd ~
# sed -i.old "s/$vimOldTheme/$vimTheme/g" .vimrc

# Sublime Text
cd /Users/cpolaris/Library/Application\ Support/Sublime\ Text\ 3/Packages/User
# General
cp Preferences.sublime-settings old-Preferences.sublime-settings
jq ".color_scheme = \"$sublimeTheme\"" < old-Preferences.sublime-settings > Preferences.sublime-settings.tmp
mv Preferences.sublime-settings.tmp Preferences.sublime-settings
# Markdown Editing
cp Markdown.sublime-settings old-Markdown.sublime-settings
jq ".color_scheme = \"$sublimeThemeMd\"" < old-Markdown.sublime-settings > Markdown.sublime-settings.tmp
mv Markdown.sublime-settings.tmp Markdown.sublime-settings
