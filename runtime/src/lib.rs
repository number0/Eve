#![feature(slice_patterns)]
#![feature(rc_unique)]
#![feature(bitset)]
#![feature(drain)]

extern crate rustc_serialize;
extern crate cbor;
extern crate websocket;
extern crate time;
extern crate hyper;
extern crate cookie;
extern crate url;
extern crate mime;

macro_rules! time {
    ($name:expr, $expr:expr) => {{
        let start = ::time::precise_time_s();
        let result = $expr;
        let end = ::time::precise_time_s();
        println!("{} took {}s", $name, end - start);
        result
    }};
}

macro_rules! string {
    ($($args:expr),*) => (::value::Value::String(format!($($args),*)))
}

pub mod value;
pub mod map;
pub mod relation;
pub mod view;
pub mod flow;
pub mod primitive;
pub mod compiler;
pub mod server;
