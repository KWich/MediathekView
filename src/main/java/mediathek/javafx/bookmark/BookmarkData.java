package mediathek.javafx.bookmark;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import mediathek.daten.DatenFilm;

/**
 * Bookmark data definition used to store movies
 * @author K. Wich
 * 
 * Note: Prepared for Jackson JSON storage
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class BookmarkData {

  private String url;
  private String sender;
  private String titel;
  private String senddate;
  private final BooleanProperty seen;
  private DatenFilm filmdata;
  private String highQualityUrl;
  private String urlKlein;
  private String note;
  private String expiry;
  private boolean willExpire;
  private boolean newlyAdded;
  private String category;
  
  public BookmarkData() {
    seen = new SimpleBooleanProperty(false);
  }
 
  public BookmarkData(DatenFilm filmdata) {
    this();
    this.url = filmdata.getUrl();
    this.sender = filmdata.getSender();
    this.titel = filmdata.getTitle();
    this.senddate = filmdata.getSendeDatum();
    this.highQualityUrl = filmdata.getHighQualityUrl();
    this.urlKlein = filmdata.getUrlKlein();
    this.filmdata = filmdata; 
    this.willExpire = false;
    this.newlyAdded = true;
  }
    
  public BookmarkData(DatenFilm filmdata, String category) {  
    this(filmdata);
    this.category = category;
  }

  // getter/setter used for Jackson
  public String getUrl(){ return this.url; }
  public void   setUrl(String url){ this.url = url; }

  public String getSender(){ return this.sender; }
  public void   setSender(String url){ this.sender = url; }

  public String getThema(){ return (filmdata != null ? filmdata.getThema(): ""); }
  public void   setThema(String url){}

  public String getTitel(){ return this.titel; }
  public void   setTitel(String url){ this.titel = url;}

  public String getDauer(){ return ((filmdata != null) ? filmdata.getDauer(): ""); }
  public void   setDauer(String dauer){}

  public String getDescription(){ return ((filmdata != null) ? filmdata.getDescription(): ""); }
  public void   setDescription(String description){}
  
  public String getNote(){ return this.note; }
  public void   setNote(String note){ this.note = note; }
  
  public String getExpiry(){ return this.expiry; }
  public void   setExpiry(String expiry){ 
    this.expiry = expiry; 
    // Check if expiry is about to happen:
    willExpire = expiry != null && BookmarkDateDiff.getInstance().diff2Today(expiry) < 4;
  }
          
  public boolean getSeen(){ return this.seen.get(); }
  public void   setSeen(boolean seen){ this.seen.set(seen);}

  public String getSendDate(){ return this.senddate; }
  public void   setSendDate(String senddate){ this.senddate = senddate; }
  
  public String getHighQualityUrl(){ return this.highQualityUrl; }
  public void   setHighQualityUrl(String highQualityUrl){ this.highQualityUrl = highQualityUrl;}
  
  public String getUrlKlein() { return urlKlein; }
  public void setUrlKlein(String urlKlein) { this.urlKlein = urlKlein; }
  
  public String getCategory(){ return this.category; }
  public void   setCategory(String category){ this.category = category; }
  
  // property access:
  @JsonIgnore
  public BooleanProperty getSeenProperty() { return seen; }
  
  // other methods:
  @JsonIgnore
  public boolean hasURL() {
    return this.url != null;
  }
  
  @JsonIgnore
  public boolean hasWebURL() {
    return (this.filmdata != null && !this.filmdata.getWebsiteLink().isEmpty());
  }

  /**
   * Compare with URL and Sender to get unique movie
   * @param url String
   * @param sender String
   * @return true if equal
   */
  @JsonIgnore
  public boolean isMovie(String url, String sender) {
    return this.url.compareTo(url) == 0 && this.sender.compareTo(sender) == 0;
  }
    
  @JsonIgnore
  public boolean isNotInFilmList() {
    return this.filmdata == null;
  }
  
  @JsonIgnore
  public boolean isLiveStream() {
    return (this.filmdata != null) ? this.filmdata.isLivestream() : false;
  }
  
  @JsonIgnore
  public boolean isNew() {
    return this.newlyAdded;
  }
    
  @JsonIgnore
  public void setDatenFilm(DatenFilm filmdata) {
    this.filmdata = filmdata;
  }
  
  @JsonIgnore
  public DatenFilm getDatenFilm() {
    return this.filmdata;
  }
  
  @JsonIgnore
  public String getWebUrl() {
    return (this.filmdata != null) ? this.filmdata.getWebsiteLink() : null;
  }

  @JsonIgnore
  public String getFormattedNote() {
    return note != null && !note.isEmpty() ? String.format("\n\nAnmerkung:\n%s", note) : "";
  }
  
  @JsonIgnore
  public String getExtendedDescription() {
    StringBuilder sb = new StringBuilder(sender);
    boolean ex = expiry != null && !expiry.isEmpty();
    sb.append(" - ");
    sb.append(getThema());
    sb.append(" - ");
    sb.append(titel);
    if (ex) {
      sb.append("     (Verfügbar bis ");
      sb.append(expiry);
    }
    if (this.filmdata != null) {
      if (ex) {
        sb.append(", ");
      } else {
        sb.append("     (");
        ex = true;
      } 
      sb.append("gesendet am ");
      sb.append(filmdata.getSendeDatum());
      sb.append(" ");
      sb.append(filmdata.getSendeZeit().subSequence(0, 5));
      sb.append(" -  Dauer ");
      sb.append(this.getDauer());
    }
    if (ex) {
      sb.append(")");
    }
    sb.append("\n\n");
    sb.append(getDescription());
    sb.append(getFormattedNote());
    return sb.toString();
  }
  
  
  /**
   * Get either the stored DatenFilm object or a new created from the internal data
   * @return DatenFilm Object
   */
  @JsonIgnore
  public DatenFilm getDataAsDatenFilm() {
    DatenFilm Film = getDatenFilm();
    if (Film == null) { // No reference in in object create new return object
      Film = new DatenFilm();
      Film.setThema(getThema());
      Film.setTitle(getTitel());
      Film.setUrl(getUrl());
      Film.setHighQualityUrl(getHighQualityUrl());
      Film.setUrlKlein(getUrlKlein());
      Film.setSender(getSender());
      Film.setDauer(getDauer());
    }
    return Film;
  }

  /**
   * Check if expiry date is about to expire
   * @return true/false
   *
   * Note: Always returns false if no expiry date is set
   */  
  @JsonIgnore
  public boolean willExpire() {
    return this.willExpire;
  }
  
  @JsonIgnore
  public boolean hasCategory(String category) {
    return this.category != null && this.category.equals(category);
  }
  
  @JsonIgnore
  public boolean hasNoCategory() {
    return this.category == null;
  }
}
