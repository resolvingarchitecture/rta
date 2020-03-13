package ra.rta.producers.file;

import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FileProducerShutdown extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(FileProducerShutdown.class);

    private List<FileSplitter> fileSplitters;
    private Producer<String, String> transactionProducer;
    private Producer<String, String> referenceProducer;
    private int maxWait = 20000;
    private int waitTime = 2000;
    private int accumulatedWait = 0;

    public FileProducerShutdown(List<FileSplitter> fileSplitters,
                                Producer<String, String> transactionProducer,
                                Producer<String, String> referenceProducer) {
        this.fileSplitters = fileSplitters;
        this.transactionProducer = transactionProducer;
        this.referenceProducer = referenceProducer;
    }

    @Override
    public void run() {
        LOG.info("File Producer stopping worker threads...");
        for(FileSplitter fileSplitter : fileSplitters)
            fileSplitter.terminate(); // Signal all FileSplitters to complete
        int fileSplittersCompleted = 0;
        do {
            try {
                LOG.info("Waiting {} seconds for thread workers to complete.",waitTime/1000);
                accumulatedWait += waitTime;
                Thread.currentThread().sleep(waitTime);
            } catch(InterruptedException ie) {
                fileSplittersCompleted = 0; // reset
                for (FileSplitter fileSplitter : fileSplitters) {
                    if (fileSplitter.getCompleted()) fileSplittersCompleted++;
                }
                LOG.info(fileSplittersCompleted + " out of " + fileSplitters.size() + " thread workers have completed.");
                if(accumulatedWait >= maxWait) {
                    LOG.info("Max wait time of {} seconds has occurred. Force exiting...",maxWait/1000);
                    break;
                }
            }
        } while(fileSplitters.size() < fileSplittersCompleted);
        transactionProducer.close();
        referenceProducer.close();
        if(accumulatedWait < maxWait)
            LOG.info("All worker threads stopped. {} gracefully shutdown.",FileProducer.class.getSimpleName());
        else
            LOG.info("{} forcefully shutdown.",FileProducer.class.getSimpleName());
    }
}