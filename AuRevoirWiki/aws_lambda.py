# coding=utf-8
from bs4 import BeautifulSoup as BS
import wikipedia
import json


def get_article(page):
    bare_html = page.html()
    soup = BS(bare_html, 'html.parser')
    payload = ''

    # Replace all "edit" links with links back to table of content.
    for atag in soup.find_all(attrs={'class':'mw-editsection'}):
        par_name = atag.parent.name
        if par_name == 'h2':
            toclink = soup.new_tag('a', href='#toctitle')
            toclink.string = 'back'
            atag.contents[1].replace_with(toclink)
        elif par_name == 'h3':
            atag.decompose()

    # Remove all external links
    for atag in soup.find_all('a'):
        try:
            ref = atag.get('href')
            if not (ref.startswith('/wiki/') or ref.startswith('#')):
                del atag['href']
        except AttributeError:
            continue

    # Remove all images
    for atag in soup.find_all('img'):
        atag.decompose()

    # Remove all thumbnail captions
    for atag in soup.find_all(attrs={'class':'thumbcaption'}):
        atag.decompose()

    # Add header
    hdr = soup.new_tag('h1')
    hdr.string = page.title
    soup.contents[0].insert_before(hdr)

    payload = str(soup)
    return payload


def lambda_handler(event, context):
    lang_choice = event['lang']
    wikipedia.set_lang(lang_choice)

    to_search = event['title'].replace('_', ' ')
    try:
        page = wikipedia.page(to_search)
        cont = get_article(page)
        suc = 'T'
    except wikipedia.exceptions.PageError:
        cont = ''
        suc = 'NM'
    except wikipedia.exceptions.DisambiguationError as er:
        probs = er.options
        cont = '<h2><b>' + to_search + '</b> may refer to:</h2><ul>'
        for li in probs:
            cont = cont + '<li><a href="/wiki/' + li.replace (' ', '_') + '">' + li + '</a></li>'
        cont = cont + '</ul>'
        suc = 'F'

    return {'suc': suc, 'content': cont}
