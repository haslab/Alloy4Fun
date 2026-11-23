package pt.haslab.alloy4fun;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import java.util.Date;

@Path("/greet")
public class AlloyService {
    @GET
    @Produces("text/plain")
    public Response doGet() {
        return Response.ok("maps 2: method doGet invoked " + new Date()).build();
    }
}

