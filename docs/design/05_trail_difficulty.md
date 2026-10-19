# 05 Trail difficulty sources

Research note for the open item in ADR 0004: can trail difficulty ratings be
imported from another open source? Short answer: not from anything found for
the Sunshine Coast, so difficulty comes from OSM tags where they exist.

Checked 2026-10-19, focused on Roberts Creek and Largo Road, BC.

## Findings

| Source | Difficulty? | Licence | Usable? |
| --- | --- | --- | --- |
| OSM tags (`mtb:scale:imba`, `mtb:scale`, `sac_scale`) | Yes, but sparse: 4%, 1% and under 1% of 755 trail ways | ODbL | Yes, already in the tile schema |
| Trailforks | Yes, good coverage | Proprietary | No (see below) |
| BC Recreation Lines (`WHSE_FOREST_TENURE.FTEN_RECREATION_LINES_SVW`) | No | OGL-BC | Geometry only, and little of it |
| BC Recreation Trails Subset, Recreation Sites and Trails BC interactive map | Not checked | "Access Only" | No |
| Recreation Opportunity Spectrum (OGL-BC) | No, it measures remoteness | OGL-BC | Not a difficulty measure |
| Local trail associations (Sunshine Coast) | Unknown | Unknown | No open dataset found |

### Trailforks

Its data policy allows use only through its API with an approved key, requires
attribution, and says copying data for non-personal use is not permitted. API
access is discretionary and, per its own page, not usually granted to
individuals for personal projects. Downloads are rate limited to 10 trails an
hour, and bulk region downloads are for regional administrators only. Scraping
ratings into the app would break those terms, so it is ruled out. Source pages:
`trailforks.com/about/data/`, `trailforks.com/about/api/` and its help article
on exporting trail data. The terms were read from search summaries, not the
full legal text, and some pages are old.

### BC government data

The Recreation Line layer is open (Open Government Licence - British Columbia,
catalogue id `recreation-line`, served by WFS at `openmaps.gov.bc.ca`). A query
over the Roberts Creek and Largo Road box returned 23 features, all
`RTR` recreation trail reserves such as Gibsons Trail and the Dakota Ridge
network. The fields are administrative (file id, project name, right of way,
district, length). There is no difficulty, use type or surface field. It could
add trail reserve names and boundaries, but not ratings, and it covers far
fewer trails than OSM.

### OSM tagging rules

`mtb:scale:imba` (0 to 4) is meant for purpose-built MTB trails and is tagged
by mappers from signed or officially assigned ratings. `mtb:scale` is for
natural trails. Mappers advise against inventing a rating for a single trail,
because ratings are relative to the other trails in the area.

## Options

1. **OSM only (recommended for the first version).** Colour trails by
   `mtb:scale:imba` or `mtb:scale` where present and a neutral colour where not.
   No new data, no licence risk, already in the schema.
2. **Improve OSM, then use it.** When on a trail with a posted rating sign,
   tag it in OSM (a signed rating is verifiable, which is what the tag
   expects). The next region rebuild picks it up and everyone benefits. Slow,
   but it keeps one data path and stays ODbL.
3. **Own ratings in the app.** A per-trail difficulty the owner sets by hand,
   stored in Room and drawn over the tiles. This is the fallback ADR 0004
   mentions. It needs a way to match a rating to an OSM way id, which survives
   rebuilds as long as the way is not split. Worth a separate design note only
   if the gaps bother in real use.
4. **Import a local association dataset.** Not available. Ask the local MTB
   association whether they publish ratings under an open licence. If they do,
   check ODbL compatibility before merging them into the tiles.

## Recommendation

Take option 1 now, note option 2 as the way to fill gaps, and revisit option 3
after using the map outdoors. Rating colours are a style decision for #21, so
they do not block it. This closes the difficulty question in ADR 0004 as
"accept the gap".

## Left unchecked

- Whether a local trail association publishes ratings.
- Open datasets outside BC. The app is for the owner's own use, so other
  regions can be checked when the owner builds a region there.
- Full Trailforks legal terms.
