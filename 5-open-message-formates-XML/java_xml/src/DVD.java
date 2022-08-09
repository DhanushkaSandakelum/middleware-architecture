import java.util.*;

public class DVD {	
	private List movies=new ArrayList();	
	public DVD(){}
	public List getMovies() {
		return movies;
	}
	public void setMovies(List movies) {
		this.movies = movies;
	}	
	public String toString(){
		String movies="";
		Movie movie = null;
		for(Object object:getMovies()){
			movie = (Movie)object;
			// Getting the released year from Movie class getters
			// seperated by a space after the name
			movies += movie.getName()+ ", "+movie.getReleased();
		}
		return movies; 
	}	
 }