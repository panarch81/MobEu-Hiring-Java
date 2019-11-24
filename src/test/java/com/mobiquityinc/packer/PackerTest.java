package com.mobiquityinc.packer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.mobiquityinc.exception.APIException;

@RunWith(MockitoJUnitRunner.class)
public class PackerTest {

	@Test(expected=APIException.class)
	public void shouldThrowsAPIExceptionWhenPathFileIsNullOrBlank() throws APIException {
		Packer.pack("");
	}
	
	@Test(expected=APIException.class)
	public void shouldThrowsAPIExceptionWhenFileContentIsEmpty() throws APIException {
		    Packer.pack("emptyFile.txt");
	}
	
	@Test 
	public void shouldExtractDataWhenFileHasOneLineContent() throws APIException {
		String expectedResult = "81 : (1,53.38,€45)";
		String data = Packer.extractData("oneThing2bePackaged.txt");
		Assert.assertEquals("theres expected data was extracted",data, expectedResult);
	}
	
	@Test 
	public void shouldExtractDataWhenFileHasTwoLinesContent() throws APIException {
		String expectedResult = "81 : (1,53.38,€45) (2,88.62,€98) (3,78.48,€3) "
							  + "(4,72.30,€76) (5,30.18,€9) (6,46.34,€48)\n"
							  + "8 : (1,15.3,€34)";
		String data = Packer.extractData("twoThings2bePackaged.txt");
		Assert.assertEquals("theres expected data was extracted",data, expectedResult);
	}
	
	@Test
	public void shouldReturnOneThingToBePackagedBetweenMany() throws APIException {
		String expectedResult = "4\n"; 
		String result = Packer.pack("oneThing2bePackagedBetweenMany.txt");
		Assert.assertEquals("theres no things to be packaged",result, expectedResult);
	}	
	
	@Test
	public void shouldReturnOneThingForFirstPackegeButNoThingForSecondPackage() throws APIException {
		String expectedResult = "4\n-\n"; 
		String result = Packer.pack("twoThings2bePackaged.txt");
		Assert.assertEquals("theres no things to be packaged",result, expectedResult);
	}
	
	@Test
	public void shouldReturnThingsForEachPackage() throws APIException {
		String expectedResult = "4\n-\n2,7\n8,9\n"; 
		String result = Packer.pack("things2bePackaged2.txt");
		Assert.assertEquals("theres no things to be packaged",result, expectedResult);
	}
}
