def is_alphabet num
  return true if (num == 97 or num == 122 or (num > 97 and num < 122))
  return true if (num == 65 or num == 90 or (num > 65 and num < 90))
  return true if (num == 45)
  return false
end

def is_all_alphabet text
  (0..text.strip.length-1).each do |i|
    return false unless is_alphabet(text.strip[i])
  end
  return true
end

def clean_prefix data
  while(not is_alphabet data[0])
    data = data [1..-1]
  end
  return data
end

@number_of_alphabet_to_use = 2
@folder = "txt/"
@dict_files ={}
@dict_file_prefix = "_mz.txt"
@txt_list = File.new('txt_list.txt','w')

def add_to_cluster cluster_name, text_data
  if @dict_files.has_key?(cluster_name)
    #File exist (read and write)
    cluster_file = File.open(@dict_files[cluster_name])
    old_data = cluster_file.read
    cluster_file.close()

    cluster_file = File.open(@dict_files[cluster_name],'w')
    cluster_file.puts(old_data.strip + "\n" + text_data)
    cluster_file.close();
    
  else
    #file missing write
    cluster_file = File.new(@folder + cluster_name + @dict_file_prefix,'w')
    cluster_file.puts(text_data)
    @dict_files[cluster_name] = @folder + cluster_name + @dict_file_prefix
    #puts "NEW CLUSTER CREATED : " + cluster_name
    @txt_list.puts "\"#{@dict_files[cluster_name]}\","
    cluster_file.close
  end

end

counter = 0;
loop_counter = 0
File.open("encsv.csv").readlines.each do |line|
  loop_counter += 1
  begin
    line = line.strip.gsub(",","|").gsub("||","|TBD|") + "||"
    eng_word = line[0..line.index('|')-1]
    eng_word = clean_prefix eng_word.strip.downcase

    #.strip is added to consolidate the case like "on" and "on " prefixes.
    if is_all_alphabet eng_word[0..@number_of_alphabet_to_use-1].strip
      add_to_cluster eng_word[0..@number_of_alphabet_to_use-1].strip, line
      counter += 1;
    else
      add_to_cluster "exception", line
      counter += 1;
    end
  rescue
    puts "ERROR:(#{loop_counter})" + line
  end
end

@txt_list.close
puts counter


