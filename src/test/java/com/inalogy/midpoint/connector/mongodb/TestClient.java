package com.inalogy.midpoint.connector.mongodb;
import org.testng.annotations.Test;


public class TestClient {
    private TestProcessor testProcessor;


    private void init() {
        testProcessor = new TestProcessor();
    }

    @Test
    public void testExec1() {
        init();
        this.testProcessor.connector.test();
        this.testProcessor.connector.schema();
    }


}
