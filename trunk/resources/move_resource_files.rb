require 'fileutils'
include FileUtils

cp 'MZDictRsc.res','../build/compiled/MZDictRsc.res'
cp 'ZawGyiBitMap_00.png','../build/compiled/ZawGyiBitMap_00.png'
cp_r 'txt','../build/compiled'
