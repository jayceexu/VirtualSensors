package bach.jianxu.watchsense;

/**
 * This function is for metaprogram.
 * In case that users want to deep-customize their sensor usage
 * by override the onSensorOverride function.
 *
 */
public class MetaFunction {

    public Double[] onSensorOverride(Double[] watchData) {
        // Start user-defined behavior of customization


        return watchData;
    }
}
