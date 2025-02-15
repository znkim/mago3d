package gaia3d.weather.json;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class CoordinateReferenceSystemTest {

    @Test
    void valueOf() throws IOException {
        // given
        File file = Paths.get("src", "test", "resources", "OBS-QWM_2019090809.grib2").toFile();
        String location = file.getAbsolutePath();
        GridDataset gridDataset = GridDataset.open(location);
        GridDatatype gridDataType = gridDataset.findGridDatatype("u-component_of_wind_isobaric");
        GridCoordSystem coordinateSystem = gridDataType.getCoordinateSystem();
        // when
        CoordinateReferenceSystem crs = CoordinateReferenceSystem.valueOf(coordinateSystem);
        log.info(crs.toString());
        // then
        assertThat(crs.getProperties().get("name")).isEqualTo("urn:ogc:def:crs:EPSG::4326");
    }

}