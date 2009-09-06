@java_code = File.new("java_code.txt",'w')
@lines = File.open("ZawGyiBitMap.fnt").readlines

# Bit Map Image Name
line2 = @lines[2]
@java_code.puts "private Image font_map = Image.createImage(this.getClass().getResourceAsStream(\"#{line2[line2.index('="')+2..-2]}\"));"

# Line Height
line1 = @lines[1]
line1_1 = line1.split(' ')[1]
line_height = line1_1[line1_1.index("=")+1..-1]
@java_code.puts "private int lineHeight = #{line_height};"

# Base Line Position
line1_2 = line1.split(' ')[2]
base = line1_2[line1_2.index("=")+1..-1]
@java_code.puts "private int linebase = #{base}"

# java.util.Hashtable fontMap = new java.util.Hashtable();
# int[] tempArray;
#
# tempArray  = {1,2,3,4,5};
# fontMap.add(key,tempArray);

i = 0;
@lines[4..-1].each do |line|
  items = line.split(' ');

  if line.index("kerning").nil?
    @java_code.puts "int[] tempArray_#{i} = {" +
      #x
    items[2].split('=')[1].strip + ", " +
      #y
    items[3].split('=')[1].strip + ", " +
      #width
    items[4].split('=')[1].strip + ", " +
      #height
    items[5].split('=')[1].strip + ", " +
      #xoffset
    items[6].split('=')[1].strip + ", " +
      #yoffset
    items[7].split('=')[1].strip + ", " +
    #xadvance
    items[8].split('=')[1].strip + "};"

     #id
    @java_code.puts "fontMap.put(new Integer(#{items[1].split('=')[1].strip}),tempArray_#{i});"
    i += 1
  end

end
@java_code.close