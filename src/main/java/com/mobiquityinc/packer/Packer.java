package com.mobiquityinc.packer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.platform.commons.util.StringUtils;

import com.mobiquityinc.exception.APIException;

/**
 * process a set of things from a file
 * converting to group of things with index, weight and price
 * and process to calculate the groups of things that may be packaged
 * in the range of weight expected with the maximun cost
 * @author paolaarenas
 *
 */
public class Packer {

  private Packer() {
  }

  /**
   * Process file to extract data
   *  with the things data
   * the expected weight in the package and the candidates things to be packaged
   * @param filePath
   * @return
   * @throws APIException
   */
  public static String pack(String filePath) throws APIException {
	String data = "";  

	if(StringUtils.isBlank(filePath))  {
		throw new APIException("incorrect parameters are being passed");
	}
	
	data = extractData(filePath);
	
	if(0 ==data.length()) throw new APIException("File is empty");

    List<String> lines=  getLinesWithoutProcess(data);
    
    String result = lines.stream().map(line -> processBunchOfThings(line) )
    							  .map(line -> line.concat("\n"))
    							  .reduce("", String::concat);
    System.out.println(result);
    return result;
  }

protected static String extractData(String filePath) throws APIException {
	Path path;
	Stream<String> lines = null;
	String data = "";
	try {
		path = Paths.get(Packer.class.getClassLoader()
		  .getResource(filePath).toURI());
		
		lines = Files.lines(path);
		data = lines.collect(Collectors.joining("\n"));
	    
	} catch (IOException e) {
		throw new APIException(e.getMessage());
	}
	catch (URISyntaxException e) {
		throw new APIException(e.getMessage());
	}
	finally {
		lines.close();
	}
	return data;
}
  
  /**
   * Extract the lines from the file, to process 
   * @param data
   * @return
   */
  private static List<String>  getLinesWithoutProcess(String data) {
	  List<String> linesWithoutProcess = new ArrayList<String>();

	  String[] linesWithoutProcess2 = data.split("\n");
	  linesWithoutProcess = Arrays.asList(linesWithoutProcess2);	  			     
	  
	 return linesWithoutProcess;
  }

  /**
   * Process the line, validating first it has the format of data expected
   * the weight expected between 1-100 and the colon
   * the groups of things with index, weight and euro price
   * if valid, process the groups of things
   * @param line
   * @return
   */
  private static String processBunchOfThings(String line){
	  String regex = "^([1-9]|\\d\\d|100)(\\s:)(\\s(\\(([1-9]|1[0-5])\\,(([1-9]|\\d\\d)\\.([1-9]|\\d\\d)|(100\\.00))\\,€([1-9]|\\d\\d|100)\\)))+$";
	  Pattern pattern = Pattern.compile(regex);
	  Matcher matcher = pattern.matcher(line);
	  if(!matcher.find()) {
		  return "-";
	  }
	  String[] weightAndThings = line.split(":");
	  double expectedWeight = Integer.valueOf(weightAndThings[0].trim());

	  String processed = processThings(weightAndThings[1].split(" "),expectedWeight);
	  return processed;
  }

  /**
   * process the things converting to thing object
   * filtering things whose weight is less o equal to the expected weight
   * and processing this things as candidate to be packaged
   * @param thingsToProcess
   * @param expectedWeight
   * @return
   */
  private static String processThings(String[] thingsToProcess, double expectedWeight) {
	List<Thing> things = Stream.of(thingsToProcess)
							.map(groupData -> convertToThing(groupData))
							.collect(Collectors.toList());
	
	List<Thing> candidates = things.stream()
					.filter(t -> t!=null && t.getWeight() <= expectedWeight)
					.collect(Collectors.toList());
	return processCostWeight(candidates,expectedWeight);
  }
  
  /**
   * order things list by price descending
   * @param candidates
   * @param expectedWeight
   * @return
   */
  private static String processCostWeight(List<Thing> candidates, double expectedWeight) {
	  List<Thing> thingsSorted = candidates.stream()
			  .sorted(Comparator.comparingInt(Thing::getPrice).reversed())
			  .collect(Collectors.toList());
	  
	  List<Thing>  choosedThings = new ArrayList<Thing>();
	  
	  thingsSorted.stream()
	  			  .forEach(t -> {
	  				  pickThings(expectedWeight, choosedThings, t);
	  			  });

	  List<String> result = choosedThings.stream()
				.map(t -> t.getIndex())
				.map(t -> t.toString())
				.collect(Collectors.toList());
	  if(result.isEmpty()) {
		  result.add("-");
	  }
	  String resultLine = result.stream().collect(Collectors.joining(","));
	  return resultLine;
  }

  /**
   * Compare weight and price to get the thing with higher price and less weight
   * and save into choosedThings list
   * @param expectedWeight
   * @param choosedThings
   * @param t
   */
  private static void pickThings(double expectedWeight, List<Thing> choosedThings, Thing t) {
	  double weightSum = choosedThings.stream()
				.mapToDouble(ct -> ct.getWeight())
				.sum();
	  int priceSum = choosedThings.stream()
				.mapToInt(ct -> ct.getPrice())
				.sum();
	  
	  if(priceSum > 0) {
		if(weightSum + t.getWeight() < expectedWeight) {
			choosedThings.add(t);
		}
		else {
			Optional<Thing> lowestPriceThing = choosedThings.stream()
					.min(Comparator.comparing(Thing::getPrice));
			if(thingHasSamePriceButLessWeight(t, lowestPriceThing)) {
				choosedThings.remove(lowestPriceThing.get());
				choosedThings.add(t);
			}
		}
	  }
	  else {
		choosedThings.add(t);
	  }
  }

  /**
   * Border case when things have same price but pick the less weight
   * @param t
   * @param lowestPriceThing
   * @return
   */
  private static boolean thingHasSamePriceButLessWeight(Thing t, Optional<Thing> lowestPriceThing) {
	  return t.getPrice() == lowestPriceThing.get().getPrice() &&
			t.getWeight() < lowestPriceThing.get().getWeight();
  }

  /**
   * Converter to Thing object
   * @param groupData
   * @return
   */
  private static Thing convertToThing(String groupData) {
	  if("".equals(groupData))
			return null;
		
		String[] thingsToProcess = groupData.split(",");
		
		int index = Integer.valueOf(thingsToProcess[0].trim().substring(1));
		double weight = Double.valueOf(thingsToProcess[1]);
		String euroPrice = thingsToProcess[2];
		String euroPriceExtracted = euroPrice.substring(1,euroPrice.length()-1);
		int priceValue = Integer.valueOf(euroPriceExtracted);
		
		Thing thingCandidate = new Thing();
		thingCandidate.setIndex(index);
		thingCandidate.setWeight(weight);
		thingCandidate.setPrice(priceValue);
		
		return thingCandidate;
  }
}
