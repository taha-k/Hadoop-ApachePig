import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import com.google.gson.*;
import java.util.ArrayList;

//TODO import necessary components

/*
*  Modify this file to combine books from the same other into
*  single JSON object. 
*  i.e. {"author": "Tobias Wells", "books": [{"book":"A die in the country"},{"book": "Dinky died"}]}
*  Beaware that, this may work on anynumber of nodes! 
*
*/

public class QueryAuthor {
  

  //TODO define variables and implement necessary components
  public static class Map
       extends Mapper<LongWritable, Text, Text, Text>{

    public void map(LongWritable key, Text value, Context context
                    ) throws IOException, InterruptedException {
      Gson gson=new Gson();
      String author, book;
      String line= value.toString();
      String[] tuple=line.split("\\n");
      try{
            for(int i=0;i<tuple.length; i++){
            
              JsonObject jobj = gson.fromJson(tuple[i], JsonObject.class);
              author=jobj.get("author").toString();
              book=jobj.get("book").toString();
              context.write(new Text(author), new Text(book));
                
            }
      } catch(Exception e){
                e.printStackTrace();
      }
    }
  }

  public static class Reduce extends Reducer<Text,Text,NullWritable,Text>{

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
      Gson gson=new Gson();
      Configuration conf = context.getConfiguration();
      String param = conf.get("Query").toString();
      if(param.equals(key.toString())){
        try{
            JsonObject obj = new JsonObject();
            JsonArray ja = new JsonArray();
            
            for(Text val : values){
              
                JsonObject jo = new JsonObject();
                jo.addProperty("book", val.toString());
                ja.add(jo);
            }

            
            obj.addProperty("author", key.toString());
            
            obj.add("books", ja);
            
            context.write(NullWritable.get(), new Text(gson.toJson(obj)));
            
        }catch(Exception e){
            e.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    conf.set("Query", args[2]);
        if (args.length != 3) {
            System.err.println("Usage: CombineBooks <in> <out>");
            System.exit(2);
        }

        Job job = new Job(conf, "QueryAuthor");
        job.setJarByClass(QueryAuthor.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}