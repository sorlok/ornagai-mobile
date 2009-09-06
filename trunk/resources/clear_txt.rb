require 'fileutils'
include FileUtils

list = Dir.glob("txt/*.txt")
list.each do |item| rm item end